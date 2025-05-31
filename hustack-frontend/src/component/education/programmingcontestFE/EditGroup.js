import React, { useState, useEffect, useMemo } from "react";
import { useHistory, useParams } from "react-router-dom";
import { useTranslation } from "react-i18next";
import {
  Box,
  Grid,
  LinearProgress,
  Stack,
  TextField,
  Typography,
  Autocomplete,
  ListItem,
  ListItemAvatar,
  ListItemText,
  Avatar,
  Popper,
  IconButton,
} from "@mui/material";
import { LoadingButton } from "@mui/lab";
import DeleteIcon from "@mui/icons-material/Delete";
import { autocompleteClasses } from "@mui/material/Autocomplete";
import { styled } from "@mui/material/styles";
import { debounce } from "@mui/material/utils";
import { request, extractErrorMessage } from "api";
import { errorNoti, successNoti } from "utils/notification";
import ProgrammingContestLayout from "./ProgrammingContestLayout";
import PrimaryButton from "../../button/PrimaryButton";
import TertiaryButton from "../../button/TertiaryButton";
import StandardTable from "component/table/StandardTable";
import { isEmpty, trim } from "lodash";
import { toFormattedDateTime } from "utils/dateutils";
import StyledSelect from "../../select/StyledSelect";
import withScreenSecurity from "../../withScreenSecurity";

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

function EditGroup() {
  const { groupId } = useParams();
  const history = useHistory();
  const { t } = useTranslation(["common", "validation"]);
  const statuses = getStatuses(t);

  const [groupDetail, setGroupDetail] = useState({
    id: "",
    name: "",
    status: "",
    description: "",
    createdBy: "",
    lastModifiedDate: "",
  });
  const [members, setMembers] = useState([]);
  const [selectedUsers, setSelectedUsers] = useState([]);
  const [searchOptions, setSearchOptions] = useState([]);
  const [keyword, setKeyword] = useState("");
  const [loading, setLoading] = useState(false);
  const [fetching, setFetching] = useState(true);

  const handleExit = () => {
    history.push(`/programming-contest/group-manager/${groupId}`);
  };

  const handleBackToList = () => {
    history.push(`/programming-contest/teacher-list-group`);
  };

  const validateSubmit = () => {
    if (!groupDetail.name) {
      errorNoti(
        t("validation:missingField", { fieldName: t("groupName") }),
        3000
      );
      return false;
    }
    if (!groupDetail.status) {
      errorNoti(
        t("validation:missingField", { fieldName: t("status") }),
        3000
      );
      return false;
    }
    return true;
  };

  const handleSubmit = () => {
    if (!validateSubmit()) return;

    setLoading(true);
    const body = {
      name: groupDetail.name,
      status: groupDetail.status,
      description: groupDetail.description,
    };

    request(
      "put",
      `/groups/${groupId}`,
      (res) => {
        setLoading(false);
        successNoti(t("common:editSuccess", { name: t("group") }), 3000);
        history.push(`/programming-contest/group-manager/${groupId}`);
      },
      {
        onError: (e) => {
          setLoading(false);
          errorNoti(extractErrorMessage(e) || t("common:error"), 3000);
        },
      },
      body
    );
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
            const data = res.data.content.map((e) => {
              const user = {
                userId: e.userLoginId,
                fullName: `${e.firstName || ""} ${e.lastName || ""}`,
              };
              if (isEmpty(trim(user.fullName))) {
                user.fullName = "Anonymous";
              }
              return user;
            });
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
    const userIds = selectedUsers.map((user) => user.userId);
    if (!userIds.length) return;

    setLoading(true);
    request(
      "post",
      `/groups/${groupId}/members`,
      (res) => {
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
  };

  const handleRemoveMember = (userId) => {
    setLoading(true);
    request(
      "delete",
      `/groups/${groupId}/members/${userId}`,
      (res) => {
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
  };

  const fetchGroupDetails = () => {
    request(
      "get",
      `/groups/${groupId}`,
      (res) => {
        const data = res.data;
        setGroupDetail({
          id: data.id,
          name: data.name,
          status: data.status,
          description: data.description || "",
          createdBy: data.createdBy,
          lastModifiedDate: data.lastModifiedDate,
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
    request(
      "get",
      `/groups/${groupId}/members`,
      (res) => {
        const membersData = res.data.map((member) => ({
          userId: member.userId,
          fullName: member.fullName || "Anonymous",
        }));
        setMembers(membersData);
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

  const columns = [
    {
      title: t("common:member"),
      field: "userId",
      minWidth: 300,
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
      title: "",
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
    fetchGroupDetails();
    fetchMembers();
  }, [groupId]);

  useEffect(() => {
    const excludeIds = members;
    delayedSearch({ keyword, exclude: excludeIds }, (results) => {
      let newOptions = [];
      if (results) {
        newOptions = [...newOptions, ...results];
      }
      setSearchOptions(newOptions);
    });
  }, [members, keyword, delayedSearch]);

  return (
    <ProgrammingContestLayout title={t("common:edit", { name: t("group") })} onBack={handleBackToList}>
      <Typography variant="h6">
        {t("generalInfo")}
      </Typography>

      <Grid container spacing={2} mt={0}>
        <Grid item xs={3}>
          <TextField
            fullWidth
            size="small"
            required
            id="groupName"
            label={t("groupName")}
            value={groupDetail.name}
            onChange={(event) => {
              setGroupDetail({ ...groupDetail, name: event.target.value });
            }}
          />
        </Grid>
        <Grid item xs={3}>
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
        <Grid item xs={3}>
          <TextField
            fullWidth
            size="small"
            disabled
            id="createdBy"
            label={t("createdBy")}
            value={groupDetail.createdBy}
          />
        </Grid>
        <Grid item xs={3}>
          <TextField
            fullWidth
            size="small"
            disabled
            id="lastModifiedDate"
            label={t("lastModifiedDate")}
            value={groupDetail.lastModifiedDate ? toFormattedDateTime(groupDetail.lastModifiedDate) : "N/A"}
          />
        </Grid>
      </Grid>

      <Box sx={{ marginTop: "24px", marginBottom: "24px" }}>
        <Typography variant="h6" sx={{ marginBottom: "8px" }}>
          {t("description")}
        </Typography>
        <TextField
          fullWidth
          size="small"
          id="description"
          label={t("description")}
          value={groupDetail.description}
          onChange={(event) => {
            setGroupDetail({ ...groupDetail, description: event.target.value });
          }}
          multiline
          rows={4}
        />
      </Box>

      <Box sx={{ marginTop: "24px" }}>
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
                placeholder="Search by ID or name"
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
            {t("common:add", { name: "Member"})}
          </PrimaryButton>
        </Stack>
        <StandardTable
          title={t("common:groupMember")}
          columns={columns}
          data={members}
          hideCommandBar
          options={{
            selection: false,
            pageSize: 5,
            search: false,
            sorting: true,
          }}
        />
      </Box>

      <Stack direction="row" spacing={2} mt={2}>
        <TertiaryButton
          variant="contained"
          loading={loading}
          onClick={handleSubmit}
          sx={{ textTransform: "capitalize" }}
        >
          {t("save", { ns: "common" })}
        </TertiaryButton>
        <TertiaryButton variant="outlined" onClick={handleExit}>
          {t("common:exit")}
        </TertiaryButton>
      </Stack>
    </ProgrammingContestLayout>
  );
}

const screenName = "SCR_EDIT_GROUP";
export default withScreenSecurity(EditGroup, screenName, true);