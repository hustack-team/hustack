
import React, { useState, useEffect, useMemo } from "react";
import { useHistory, useParams } from "react-router-dom";
import { useTranslation } from "react-i18next";
import {
  Box,
  Divider,
  Grid,
  TextField,
  Typography,
  Autocomplete,
  ListItem,
  ListItemAvatar,
  ListItemText,
  Avatar,
  Popper,
  IconButton,
  Stack,
  Paper,
} from "@mui/material";
import DeleteIcon from "@mui/icons-material/Delete";
import { autocompleteClasses } from "@mui/material/Autocomplete";
import { styled } from "@mui/material/styles";
import { debounce } from "@mui/material/utils";
import { LoadingButton } from "@mui/lab";
import { LinearProgress, makeStyles } from "@material-ui/core";
import { request } from "api";
import { errorNoti, successNoti } from "utils/notification";
import { sleep } from "./lib";
import ProgrammingContestLayout from "./ProgrammingContestLayout";
import StyledSelect from "../../select/StyledSelect";
import StandardTable from "component/table/StandardTable";
import TertiaryButton from "../../button/TertiaryButton";
import withScreenSecurity from "../../withScreenSecurity";
import { isEmpty, trim } from "lodash";
import PrimaryButton from "component/button/PrimaryButton";
import AddIcon from "@material-ui/icons/Add";

const useStyles = makeStyles((theme) => ({
  description: {
    marginTop: theme.spacing(3),
    marginBottom: theme.spacing(3),
  },
}));

const StyledAutocompletePopper = styled(Popper)(({ theme }) => ({
  [`& .${autocompleteClasses.paper}`]: {
    boxShadow:
      "0 12px 28px 0 rgba(0, 0, 0, 0.2), 0 2px 4px 0 rgba(0, 0, 0, 0.1), inset 0 0 0 1px rgba(255, 255, 255, 0.5)",
    margin: 0,
    padding: 8,
    borderRadius: 8,
  },
  [`& .${autocompleteClasses.listbox}`]: {
    padding: 0,
    [`& .${autocompleteClasses.option}`]: {
      padding: "0px 8px",
      borderRadius: 8,
      "&:hover": {
        backgroundColor: "#eeeeee",
      },
    },
  },
}));

function PopperComponent(props) {
  return <StyledAutocompletePopper {...props} />;
}

function stringToColor(string) {
  if (!string) return "#000";
  let hash = 0;
  for (let i = 0; i < string.length; i += 1) {
    hash = string.charCodeAt(i) + ((hash << 5) - hash);
  }
  let color = "#";
  for (let i = 0; i < 3; i += 1) {
    const value = (hash >> (i * 8)) & 0xff;
    color += `00${value.toString(16)}`.slice(-2);
  }
  return color;
}

function stringAvatar(id, name) {
  const text = name
    ?.split(" ")
    .filter((word) => word)
    .map((word) => word[0])
    .join("")
    .slice(0, 2) || id.slice(0, 2);
  return {
    children: text?.toUpperCase(),
    sx: {
      bgcolor: stringToColor(id),
    },
  };
}

const getStatuses = (t) => [
  { label: t("common:statusActive"), value: "ACTIVE" },
  { label: t("common:statusInactive"), value: "INACTIVE" },
];

function GroupForm() {
  const { groupId } = useParams();
  const { t } = useTranslation(["common", "validation"]);
  const history = useHistory();
  const classes = useStyles();
  const isEditMode = !!groupId;

  const statuses = getStatuses(t);

  const [groupDetail, setGroupDetail] = useState({
    name: "",
    status: "ACTIVE",
    description: "",
  });
  const [selectedMembers, setSelectedMembers] = useState([]);
  const [searchOptions, setSearchOptions] = useState([]);
  const [keyword, setKeyword] = useState("");
  const [selectedUsers, setSelectedUsers] = useState([]);
  const [loading, setLoading] = useState(false);
  const [fetching, setFetching] = useState(isEditMode);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(5);

  const isValidGroupName = () => {
    return new RegExp(/[%^/\\|.?\[\];]/g).test(groupDetail.name);
  };

  const hasSpecialCharacterGroupName = () => {
    return !new RegExp(/^[0-9a-zA-Z ]*$/).test(groupDetail.name);
  };

  const validateSubmit = () => {
    if (!groupDetail.name) {
      errorNoti(t("validation:missingField", { fieldName: t("groupName") }), 3000);
      return false;
    }
    if (hasSpecialCharacterGroupName()) {
      errorNoti("Group Name must only contain alphanumeric characters and spaces.", 3000);
      return false;
    }
    if (isEditMode && !groupDetail.status) {
      errorNoti(t("validation:missingField", { fieldName: t("status") }), 3000);
      return false;
    }
    return true;
  };

  const delayedSearch = useMemo(
    () =>
      debounce(({ keyword, exclude }, callback) => {
        const url = keyword
          ? `/users?size=10&page=0&keyword=${encodeURIComponent(keyword)}${
              exclude ? exclude.map((user) => "&exclude=" + user.userId).join("") : ""
            }`
          : `/users?size=10&page=0${exclude ? exclude.map((user) => "&exclude=" + user.userId).join("") : ""}`;

        request(
          "get",
          url,
          (res) => {
            const data = res.data.content.map((e) => ({
              userId: e.userLoginId,
              fullName: `${e.firstName || ""} ${e.lastName || ""}`.trim() || "Anonymous",
            }));
            callback(data);
          },
          (error) => {
            errorNoti("Failed to fetch users", 3000);
            console.error("Error fetching users:", error);
          }
        );
      }, 400),
    []
  );

  const handleAddMembers = () => {
    if (isEditMode) {
      const userIds = selectedUsers.map((user) => user.userId);
      if (!userIds.length) return;

      setLoading(true);
      request(
        "post",
        `/groups/${groupId}/members`,
        () => {
          successNoti("Users were successfully added to the group", 3000);
          setSelectedUsers([]);
          setKeyword("");
          setSearchOptions([]);
          fetchMembers();
          setLoading(false);
        },
        {
          onError: (error) => {
            errorNoti("Failed to add users to the group", 3000);
            console.error("Error adding members:", error);
            setLoading(false);
          },
        },
        userIds
      );
    } else {
      const newMembers = selectedUsers.filter(
        (user) => !selectedMembers.some((m) => m.userId === user.userId)
      );
      setSelectedMembers([...selectedMembers, ...newMembers]);
      setSelectedUsers([]);
      setSearchOptions([]);
      setKeyword("");
    }
  };

  const handleRemoveMember = (userId) => {
    if (isEditMode) {
      setLoading(true);
      request(
        "delete",
        `/groups/${groupId}/members/${userId}`,
        () => {
          successNoti("Member removed successfully", 3000);
          fetchMembers();
          setLoading(false);
        },
        {
          onError: (err) => {
            errorNoti(err?.response?.data?.message || "Failed to remove member", 3000);
            setLoading(false);
          },
        }
      );
    } else {
      setSelectedMembers(selectedMembers.filter((m) => m.userId !== userId));
    }
  };

  const fetchGroupDetails = () => {
    if (!isEditMode) return;
    setFetching(true);
    request(
      "get",
      `/groups/${groupId}`,
      (res) => {
        const data = res.data;
        setGroupDetail({
          name: data.name,
          status: data.status,
          description: data.description || "",
        });
        setFetching(false);
      },
      {
        onError: (err) => {
          errorNoti("Failed to fetch group details", 3000);
          console.error("Error fetching group details:", err);
          setFetching(false);
        },
      }
    );
  };

  const fetchMembers = () => {
    if (!isEditMode) return;
    request(
      "get",
      `/groups/${groupId}/members`,
      (res) => {
        const membersData = res.data.map((member) => ({
          userId: member.userId,
          fullName: member.fullName || "Anonymous",
        }));
        setSelectedMembers(membersData);
      },
      {
        onError: (err) => {
          if (err.response?.status === 404) {
            errorNoti("Group not found", 3000);
          } else if (err.response?.status === 403) {
            errorNoti("You are not authorized to view group members", 3000);
          } else {
            errorNoti("Failed to fetch members", 3000);
          }
          console.error("Error fetching members:", err);
        },
      }
    );
  };

  const handleSubmit = () => {
    if (!validateSubmit()) return;

    setLoading(true);
    const body = isEditMode
      ? {
          name: groupDetail.name,
          status: groupDetail.status,
          description: groupDetail.description,
        }
      : {
          name: groupDetail.name,
          status: groupDetail.status,
          description: groupDetail.description,
          userIds: selectedMembers.map((m) => m.userId),
        };

    const method = isEditMode ? "put" : "post";
    const url = isEditMode ? `/groups/${groupId}` : "/groups";

    request(
      method,
      url,
      () => {
        successNoti(t(`common:${isEditMode ? "editSuccess" : "addSuccess"}`, { name: t("group") }), 3000);
        sleep(1000).then(() => {
          history.push(isEditMode ? `/programming-contest/group-manager/${groupId}` : "/programming-contest/teacher-list-group");
        });
      },
      {
        onError: (err) => {
          errorNoti(err?.response?.data?.message || t("common:error"), 5000);
          setLoading(false);
        },
      },
      body
    ).finally(() => setLoading(false));
  };

  const handleCancel = () => {
    history.push(isEditMode ? `/programming-contest/group-manager/${groupId}` : "/programming-contest/teacher-list-group");
  };

  const handleChangePage = (newPage) => {
    setPage(newPage);
  };

  const handleChangeRowsPerPage = (newSize) => {
    setPage(0);
    setPageSize(newSize);
  };

  const columns = [
    {
      title: t("common:member"),
      field: "userId",
      minWidth: 70,
      maxWidth: 280,
      cellStyle: { padding: "0 4px" },
      render: (rowData) => (
        <Stack direction="row" alignItems="center">
          <ListItemAvatar>
            <Avatar
              alt="account avatar"
              {...stringAvatar(rowData.userId, rowData.fullName)}
            />
          </ListItemAvatar>
          <ListItemText
            primary={rowData.fullName}
            secondary={rowData.userId}
          />
        </Stack>
      ),
    },
    {
      title: t("common:operation"),
      minWidth: 120,
      cellStyle: { padding: "0 4px" },
      render: (row) => (
        <IconButton
          onClick={() => handleRemoveMember(row.userId)}
          disabled={loading}
          color="error"
        >
          <DeleteIcon />
        </IconButton>
      ),
    },
  ];

  useEffect(() => {
    if (isEditMode) {
      fetchGroupDetails();
      fetchMembers();
    }
  }, [groupId]);

  useEffect(() => {
    const excludeIds = selectedMembers;
    delayedSearch({ keyword, exclude: excludeIds }, (results) => {
      setSearchOptions(results || []);
    });
  }, [selectedMembers, keyword, delayedSearch]);

  return (
    <ProgrammingContestLayout
      title={t(`common:${isEditMode ? "edit" : "create"}`, { name: t("group") })}
      onBack={handleCancel}
    >
      {fetching ? (
        <Box sx={{ width: "100%" }}>
          <LinearProgress />
        </Box>
      ) : (
        <>
          <Typography variant="h6">{t("common:generalInfo")}</Typography>

          <Grid container spacing={2} mt={0} alignItems="flex-end">
            <Grid item xs={6}>
              <TextField
                fullWidth
                size="small"
                autoFocus
                required
                id="groupName"
                label={t("groupName")}
                value={groupDetail.name}
                onChange={(event) => {
                  setGroupDetail({ ...groupDetail, name: event.target.value });
                }}
                error={isValidGroupName()}
                helperText={
                  isValidGroupName()
                    ? "Group Name must not contain special characters including %^/\\|.?\[\];"
                    : ""
                }
                sx={{ marginBottom: "12px" }}
              />
              <StyledSelect
                fullWidth
                required
                key={t("status")}
                label={t("status")}
                options={statuses}
                value={groupDetail.status}
                sx={{ minWidth: "unset", mr: "unset" }}
                onChange={(event) => {
                  setGroupDetail({ ...groupDetail, status: event.target.value });
                }}
              />
            </Grid>
            <Grid item xs={6}>
              <TextField
                fullWidth
                multiline
                rows={2}
                value={groupDetail.description}
                id="description"
                label={t("description")}
                onChange={(event) => {
                  setGroupDetail({ ...groupDetail, description: event.target.value });
                }}
                sx={{
                  "& .MuiInputBase-root": {
                    height: "92px",
                  },
                }}
              />
            </Grid>
          </Grid>

          <Box sx={{ marginTop: "20px" }}>
            <Typography variant="h6" sx={{ marginBottom: "8px" }}>
              {t("common:addMember")}
            </Typography>
            <Stack spacing={2}>
              <Autocomplete
                id="add-group-members"
                multiple
                fullWidth
                size="small"
                PopperComponent={PopperComponent}
                getOptionLabel={(option) => option.fullName || ""}
                filterOptions={(x) => x}
                options={searchOptions}
                noOptionsText="No matches found"
                value={selectedUsers}
                onChange={(event, newValue) => {
                  setSelectedUsers(newValue);
                }}
                onInputChange={(event, newInputValue) => {
                  setKeyword(newInputValue);
                }}
                renderInput={(params) => (
                  <TextField
                    {...params}
                    label={t("common:searchMember")}
                    placeholder={t("common:searchMember")}
                    inputProps={{
                      ...params.inputProps,
                      autoComplete: "new-password",
                    }}
                    disabled={loading}
                  />
                )}
                renderOption={(props, option) => (
                  <ListItem {...props} key={option.userId} sx={{ p: 0 }}>
                    <ListItemAvatar>
                      <Avatar
                        alt="account avatar"
                        {...stringAvatar(option.userId, option.fullName)}
                      />
                    </ListItemAvatar>
                    <ListItemText
                      primary={option.fullName}
                      secondary={option.userId}
                    />
                  </ListItem>
                )}
              />
              <PrimaryButton
                disabled={loading || !selectedUsers.length}
                onClick={handleAddMembers}
                sx={{ alignSelf: "flex-start" }}
              >
                {t("common:addMember")}
              </PrimaryButton>
            </Stack>
            <Divider sx={{ mt: 2, mb: 2 }} />
            <Stack direction="row" justifyContent="space-between" mb={1.5}>
              <Typography variant="h6">{t("common:groupMember")}</Typography>
              <PrimaryButton
                disabled
                startIcon={<AddIcon />}
                sx={{ visibility: "hidden" }}
              >
                {t("common:create", { name: "" })}
              </PrimaryButton>
            </Stack>
            <StandardTable
              columns={columns}
              data={selectedMembers.slice(page * pageSize, (page + 1) * pageSize)}
              hideCommandBar
              hideToolBar
              options={{
                selection: false,
                pageSize,
                pageSizeOptions: [5, 10, 20],
                pagination: true,
                serverSide: false,
                totalCount: selectedMembers.length,
                page,
                search: false,
                sorting: true,
                elevation: 0,
              }}
              components={{
                Container: (props) => <Paper {...props} elevation={0} />,
              }}
              isLoading={loading}
              page={page}
              totalCount={selectedMembers.length}
              onChangePage={handleChangePage}
              onChangeRowsPerPage={handleChangeRowsPerPage}
            />
          </Box>

          <Box width="100%" sx={{ marginTop: "20px" }}>
            <Stack direction="row" spacing={2.5} justifyContent="flex-start">
              <TertiaryButton
                variant="outlined"
                onClick={handleCancel}
                sx={{ textTransform: "capitalize" }}
              >
                {t("common:cancel")}
              </TertiaryButton>
              <LoadingButton
                variant="contained"
                loading={loading}
                onClick={handleSubmit}
                sx={{ textTransform: "capitalize" }}
                disabled={isValidGroupName() || loading || !groupDetail.name.trim()}
              >
                {t("common:save")}
              </LoadingButton>
            </Stack>
          </Box>
        </>
      )}
    </ProgrammingContestLayout>
  );
}

const screenName = "SCR_GROUP_FORM";
export default withScreenSecurity(GroupForm, screenName, true);