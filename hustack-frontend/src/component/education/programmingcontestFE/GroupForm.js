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
import LinearProgress from '@mui/material/LinearProgress';
import Tooltip from '@mui/material/Tooltip';
import { textAlign, width } from "@mui/system";
import { makeStyles } from "@material-ui/core";
import { stringAvatar, StyledAutocompletePopper } from "./AddMember2Contest";
import { stringToColor } from "./GroupManager";

const useStyles = makeStyles((theme) => ({
  description: {
    marginTop: theme.spacing(3),
    marginBottom: theme.spacing(3),
  },
}));

function GroupForm() {
  const { groupId } = useParams();
  const { t } = useTranslation(["common", "validation"]);
  const history = useHistory();
  const classes = useStyles();
  const isEditMode = !!groupId;

  const [groupDetail, setGroupDetail] = useState({
    name: "",
    description: "",
  });
  const [selectedMembers, setSelectedMembers] = useState([]);
  const [searchOptions, setSearchOptions] = useState([]);
  const [keyword, setKeyword] = useState("");
  const [selectedUsers, setSelectedUsers] = useState([]);
  const [loading, setLoading] = useState(false);
  const [fetching, setFetching] = useState(isEditMode);

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
      errorNoti(t("common:invalidGroupName"), 3000);
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
            errorNoti(t("common:fetchUsersError"), 3000);
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
          setSelectedUsers([]);
          setKeyword("");
          setSearchOptions([]);
          fetchMembers();
          setLoading(false);
        },
        {
          onError: (error) => {
            errorNoti(t("common:addUserFail"), 3000);
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
          fetchMembers();
          setLoading(false);
        },
        {
          onError: (err) => {
            errorNoti(t("common:removeUserFail"), 3000);
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
          description: data.description || "",
        });
        setFetching(false);
      },
      {
        onError: (err) => {
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
          description: groupDetail.description,
        }
      : {
          name: groupDetail.name,
          description: groupDetail.description,
          userIds: selectedMembers.map((m) => m.userId),
        };

    const method = isEditMode ? "put" : "post";
    const url = isEditMode ? `/groups/${groupId}` : "/groups";

    request(
      method,
      url,
      () => {
        successNoti(t(`common:${isEditMode ? "editSuccess" : "addSuccess"}`, { name: t("common:group2") }), 3000);
        sleep(1000).then(() => {
          history.push(isEditMode ? `/programming-contest/group-manager/${groupId}` : "/programming-contest/teacher-list-group");
        });
      },
      {
        onError: (err) => {
          errorNoti(err?.response?.data?.message || t("common:groupOperationError"), 5000);
          setLoading(false);
        },
      },
      body
    ).finally(() => setLoading(false));
  };

  const handleCancel = () => {
    history.push(isEditMode ? `/programming-contest/group-manager/${groupId}` : "/programming-contest/teacher-list-group");
  };

  const columns = [
    {
      title: t("common:member"),
      field: "userId",
      cellStyle: {width: 300},
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
      title: t("common:action"),
      sorting: false,
      cellStyle: { 
        width: 50,
        textAlign: "center"  
      }, 
      headerStyle: {
        textAlign: "center"   
      },
      render: (row) => (
        <Tooltip title={t('common:delete')}>
            <IconButton
              onClick={() => handleRemoveMember(row.userId)}
              disabled={loading}
              color="error"
            >
              <DeleteIcon />
            </IconButton>
        </Tooltip>
        
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

          <Grid container spacing={2} mt={0} alignItems="flex-start">
          <Grid item xs={3}>
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
            />
          </Grid>
          <Grid item xs={9}>
            <TextField
              fullWidth
              size="small"
              id="description"
              label={t("description")}
              value={groupDetail.description}
              onChange={(event) => {
                setGroupDetail({ ...groupDetail, description: event.target.value });
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
                PopperComponent={StyledAutocompletePopper}
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
            <Stack direction="row" justifyContent="space-between" mb={1.5} mt={1.5}>
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
              data={selectedMembers} 
              hideCommandBar
              hideToolBar
              options={{
                selection: false,
                pageSize: 5,
                search: false,
                sorting: true,
                elevation: 0,
              }}
              components={{
                Container: (props) => <Paper {...props} elevation={0} />,
              }}
              isLoading={loading}
            />
          </Box>

          <Box width="100%" sx={{ marginTop: "40px" }}>
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