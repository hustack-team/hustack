import SearchIcon from "@mui/icons-material/Search";
import { Button, InputBase, Tooltip } from "@mui/material";
import AppBar from "@mui/material/AppBar";
import Box from "@mui/material/Box";
import Toolbar from "@mui/material/Toolbar";
import { request } from "api";
import StandardTable from "component/table/StandardTable";
import withScreenSecurity from "component/withScreenSecurity";
import { useEffect, useMemo, useState } from "react";
import { useParams } from "react-router-dom";
import { PROBLEM_ROLE } from "utils/constants";
import { errorNoti, successNoti } from "utils/notification";
import { Search, SearchIconWrapper } from "./lib";
import {
  Autocomplete,
  Avatar,
  ListItem,
  ListItemAvatar,
  ListItemText,
  Popper,
  Stack,
  TextField,
  IconButton,
} from "@mui/material";
import DeleteIcon from "@mui/icons-material/Delete";
import { autocompleteClasses } from "@mui/material/Autocomplete";
import { styled } from "@mui/material/styles";
import { debounce } from "@mui/material/utils";
import { makeStyles } from "@material-ui/core/styles";
import PrimaryButton from "component/button/PrimaryButton";
import StyledSelect from "component/select/StyledSelect";
import { Group } from "@mui/icons-material";
import { isEmpty, trim } from "lodash";
import { stringAvatar, StyledAutocompletePopper } from "./AddMember2Contest";
import { useTranslation } from "react-i18next";

function PopperComponent(props) {
  return <StyledAutocompletePopper {...props} />;
}


const useStyles = makeStyles((theme) => ({
  btn: { margin: "4px 8px" },
}));



function UserContestProblemRole() {
  const { t } = useTranslation(["common", "validation"]);
  const roles = [
    {
    label: t("common:owner"),
    value: PROBLEM_ROLE.OWNER,
    },
    {
      label: t("common:editor"),
      value: PROBLEM_ROLE.EDITOR,
    },
    {
      label: t("common:viewer"),
      value: PROBLEM_ROLE.VIEWER,
    },
  ];

  const { problemId } = useParams();
  const classes = useStyles();
  const [userRoles, setUserRoles] = useState([]);
  const [value, setValue] = useState([]);
  const [options, setOptions] = useState([]);
  const [keyword, setKeyword] = useState("");
  const [selectedRole, setSelectedRole] = useState(roles[1].value);

  const delayedSearch = useMemo(
    () =>
      debounce(({ keyword, excludeUsers, excludeGroups }, callback) => {
        Promise.all([
          searchUsers({ keyword, excludeUsers, pageSize: 10, page: 1 }),
          searchGroups({ keyword, excludeGroups, pageSize: 10, page: 1 }),
        ]).then(([userResults, groupResults]) => {
          callback([...userResults, ...groupResults]);
        });
      }, 400),
    []
  );

  const columnUserRoles = [
    {
      title: t("common:member"),
      field: "userLoginId",
      cellStyle: { width: 300 },
      render: (rowData) => (
        <Stack direction="row" alignItems="center">
          <ListItemAvatar>
            <Avatar
              alt="account avatar"
              {...stringAvatar(rowData.userLoginId, rowData.fullname)}
            />
          </ListItemAvatar>
          <ListItemText
            primary={rowData.fullname}
            secondary={rowData.userLoginId}
          />
        </Stack>
      ),
    },
    {
      title: t("common:role"),
      field: "roleId",
      render: (rowData) => 
        roles.find((role) => role.value === rowData.roleId)?.label || rowData.roleId,
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
            onClick={() => handleRemove(row.userLoginId, row.roleId)}
            color="error"
          >
            <DeleteIcon />
          </IconButton>
        </Tooltip>
      ),
    },
  ];

  function handleRemove(userId, roleId) {
    let body = {
      problemId: problemId,
      userId: userId,
      roleId: roleId,
    };
    const roleLabel = roles.find((role) => role.value === roleId)?.label || roleId;
    request(
      "delete",
      "/problems/users/role",
      (res) => {
        if (res.data)
          successNoti(t("common:removeRoleUser"), 3000);
        else
          errorNoti(
            `Cannot remove user ${userId} with role ${roleLabel} from the problem`,
            3000
          );
        getUserRoles();
      },
      {
        500: () => {
          errorNoti("Server error", 3000);
        },
      },
      body
    ).then();
  }

  function onAddMembers() {
    const userIds = value
      .filter((item) => item.type === "user")
      .map((user) => user.id);
    const groupIds = value
      .filter((item) => item.type === "group")
      .map((group) => group.id);

    let body = {
      problemId: problemId,
      userIds: userIds,
      groupIds: groupIds,
      roleId: selectedRole,
    };

    request(
      "post",
      "/problems/users/role",
      (res) => {
        successNoti(t("common:addRoleUser"), 3000);
        getUserRoles();
        setValue([]);
      },
      {
        400: (err) => {
          errorNoti("Bad request: " + (err?.response?.data?.message || "Unknown error"), 3000);
        },
        500: () => {
          errorNoti("Server error", 3000);
        },
      },
      body
    ).then();
  } 

  function getUserRoles() {
    request("get", "/problems/" + problemId + "/users/role", (res) => {
      setUserRoles(res.data);
    }).then();
  }

  function searchUsers({ keyword, excludeUsers, pageSize, page }) {
    return new Promise((resolve) => {
      request(
        "get",
        `/users?size=${pageSize}&page=${page - 1}&keyword=${encodeURIComponent(keyword)}${
          excludeUsers
            ? excludeUsers.map((id) => "&exclude=" + id).join("")
            : ""
        }`,
        (res) => {
          const data = res.data.content.map((e) => {
            const user = {
              id: e.userLoginId,
              name: `${e.firstName || ""} ${e.lastName || ""}`,
              type: "user",
            };
            if (isEmpty(trim(user.name))) {
              user.name = "Anonymous";
            }
            return user;
          });
          resolve(data);
        }
      );
    });
  }

  function searchGroups({ keyword, excludeGroups, pageSize, page }) {
    return new Promise((resolve) => {
      request(
        "get",
        `/groups?size=${pageSize}&page=${page - 1}&keyword=${encodeURIComponent(keyword)}${
          excludeGroups
            ? excludeGroups.map((id) => "&exclude=" + id).join("")
            : ""
        }`,
        (res) => {
          const data = res.data.content
            .filter((e) => e.memberCount > 0)
            .map((e) => ({
              id: e.id,
              name: e.name,
              memberCount: e.memberCount,
              description: e.description,
              type: "group",
            }));
          resolve(data);
        }
      );
    });
  }

  const getGroupDisplayName = (group) => {
    const memberText = group.memberCount === 1 ? t("common:countMember") : t("common:countMembers");
    return `${group.name} (${group.memberCount} ${memberText})`;
  };

  const formatDescription = (description) => {
    if (!description) return "";
    return description.replace(/\n/g, " ").trim();
  };

  useEffect(() => {
    getUserRoles();
  }, []);

  useEffect(() => {
    let active = true;

    const excludeUsers = value.filter((item) => item.type === "user").map((item) => item.id);
    const excludeGroups = value.filter((item) => item.type === "group").map((item) => item.id);

    delayedSearch({ keyword, excludeUsers, excludeGroups }, (results) => {
      if (active) {
        setOptions(results);
      }
    });

    return () => {
      active = false;
    };
  }, [value, keyword, delayedSearch]);

  return (
    <div>
      <Stack
        spacing={3}
        alignItems={"flex-start"}
        sx={{
          p: 2,
          mb: 2,
          backgroundColor: "#ffffff",
          boxShadow: 1,
          borderRadius: 2,
        }}
      >
        <Autocomplete
          id="add-members"
          multiple
          fullWidth
          size="small"
          PopperComponent={PopperComponent}
          getOptionLabel={(option) =>
            option.type === "group" ? getGroupDisplayName(option) : option.name
          }
          filterOptions={(x) => x} 
          options={options}
          value={value}
          noOptionsText="No matches found"
          onChange={(event, newValue) => {
            setValue(newValue);
            setOptions(newValue.concat(options.filter((opt) => !newValue.some((v) => v.id === opt.id))));
          }}
          onInputChange={(event, newInputValue) => {
            setKeyword(newInputValue);
          }}
          renderInput={(params) => (
            <TextField
              {...params}
              label={t("common:searchMemberGroupTitle")}
              placeholder={t("common:searchMemberGroup")}
              inputProps={{
                ...params.inputProps,
                autoComplete: "new-password",
              }}
            />
          )}
          renderOption={(props, option, { selected }) => (
            <ListItem
              {...props}
              key={option.id}
              sx={{ p: 0, color: selected && option.type === "group" ? "#1976d2" : "inherit" }}
            >
              {option.type === "user" ? (
                <ListItemAvatar>
                  <Avatar
                    alt="account avatar"
                    {...stringAvatar(option.id, option.name)}
                  />
                </ListItemAvatar>
              ) : (
                <ListItemAvatar>
                  <Avatar sx={{ bgcolor: "#1976d2" }}>
                    <Group/>
                  </Avatar>
                </ListItemAvatar>
              )}
              <ListItemText
                primary={option.type === "group" ? getGroupDisplayName(option) : option.name}
                secondary={option.type === "group" ? formatDescription(option.description) : option.id}
                secondaryTypographyProps={{
                  sx: {
                    whiteSpace: "nowrap",
                    overflow: "hidden",
                    textOverflow: "ellipsis",
                  }
                }}
              />
            </ListItem>
          )}
        />
        <StyledSelect
          required
          key={"Role"}
          label={t("common:role")}
          options={roles.filter(role => role.value !== PROBLEM_ROLE.OWNER)}
          value={selectedRole}
          onChange={(e) => {
            setSelectedRole(e.target.value);
          }}
        />
        <Stack direction={"row"} spacing={2}>
          <PrimaryButton
            disabled={value.length === 0}
            className={classes.btn}
            onClick={onAddMembers}
          >
            {t("common:share")}
          </PrimaryButton>
        </Stack>
      </Stack>

      <Box sx={{ margin: "1.5rem" }} />
      <StandardTable
        title={t("common:userAndRoles")}
        columns={columnUserRoles}
        data={userRoles}
        hideCommandBar
        options={{
          selection: false,
          pageSize: 10,
          search: true,
          sorting: true,
        }}
      />
    </div>
  );
}

const screenName = "SCR_USER_CONTEST_PROBLEM_ROLE";
export default withScreenSecurity(UserContestProblemRole, screenName, true);