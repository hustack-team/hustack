import {
  Autocomplete,
  Avatar,
  Divider,
  IconButton,
  ListItem,
  ListItemAvatar,
  ListItemText,
  Paper,
  Stack,
  TextField,
  Tooltip
} from "@mui/material";
import {request} from "api";
import StandardTable from "component/table/StandardTable";
import withScreenSecurity from "component/withScreenSecurity";
import {useEffect, useMemo, useState} from "react";
import {useHistory, useParams} from "react-router-dom";
import {PROBLEM_ROLE} from "utils/constants";
import {errorNoti, successNoti} from "utils/notification";
import DeleteIcon from "@mui/icons-material/Delete";
import {debounce, isEmpty, trim} from "lodash";
import {makeStyles} from "@material-ui/core/styles";
import PrimaryButton from "component/button/PrimaryButton";
import StyledSelect from "component/select/StyledSelect";
import {Group, Share} from "@mui/icons-material";
import {stringAvatar, StyledAutocompletePopper} from "./AddMember2Contest";
import {useTranslation} from "react-i18next";
import ProgrammingContestLayout from "./ProgrammingContestLayout";

const useStyles = makeStyles((theme) => ({
  btn: {margin: "4px 8px"},
}));

function UserContestProblemRole() {
  const {t} = useTranslation(["common", "validation"]);
  const {problemId} = useParams();
  const history = useHistory();

  const roles = useMemo(() => [
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
  ], [t]);

  const classes = useStyles();
  const [userRoles, setUserRoles] = useState([]);
  const [value, setValue] = useState([]);
  const [options, setOptions] = useState([]);
  const [keyword, setKeyword] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const DEFAULT_ROLE = roles[1].value;
  const [selectedRole, setSelectedRole] = useState(DEFAULT_ROLE);

  const handleBack = () => {
    history.push("/programming-contest/manager-view-problem-detail/" + problemId);
  };

  const handleError = (e) => {
    if (e.response && e.response.status === 403) {
      history.push("/programming-contest/list-problems");
    } else {
      errorNoti(t("common:error"), 3000);
    }
  };

  const delayedSearch = useMemo(
    () =>
      debounce(({keyword, excludeUsers, excludeGroups}, callback) => {
        Promise.all([
          searchUsers({keyword, excludeUsers, pageSize: 10, page: 1}),
          searchGroups({keyword, excludeGroups, pageSize: 10, page: 1}),
        ]).then(([userResults, groupResults]) => {
          callback([...userResults, ...groupResults]);
        });
      }, 400),
    []
  );

  const columnUserRoles = [
    {
      title: t("common:user"),
      field: "userLoginId",
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
        row.roleId !== PROBLEM_ROLE.OWNER && (
          <Tooltip title={t('common:revoke')}>
            <IconButton
              aria-label={t('common:revokePermission')}
              onClick={() => handleRemove(row.userLoginId, row.roleId)}
              color="error"
            >
              <DeleteIcon/>
            </IconButton>
          </Tooltip>
        )
      ),
    },
  ];

  function handleRemove(userId, roleId) {
    const body = {
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
        onError: handleError,
      },
      body
    );
  }

  function onAddMembers() {
    setIsLoading(true);
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
        setIsLoading(false);
        if (groupIds.length > 0) {
          successNoti(t("common:addGroupRoleSuccess"), 3000);
        } else {
          successNoti(t("common:addRoleUser"), 3000);
        }
        getUserRoles();
        setValue([]);
      },
      {
        400: (err) => {
          setIsLoading(false);
          if (groupIds.length > 0) {
            errorNoti(t("common:addGroupRoleError", {message: err?.response?.data?.message || t("common:error")}), 3000);
          } else {
            errorNoti(t("common:badRequest") + ": " + (err?.response?.data?.message || t("common:error")), 3000);
          }
        },
        onError: (e) => {
          setIsLoading(false);
          handleError(e);
        },
      },
      body
    );
  }

  function getUserRoles() {
    request("get", "/problems/" + problemId + "/users/role", (res) => {
      setUserRoles(res.data);
    }, {
      onError: handleError,
    });
  }

  const searchEntities = (type, {keyword, excludeIds, pageSize, page}) => {
    const endpoint = type === 'user' ? 'users' : 'groups';
    const excludeParam = excludeIds ? excludeIds.map((id) => "&exclude=" + id).join("") : "";

    return new Promise((resolve) => {
      request(
        "get",
        `/${endpoint}?size=${pageSize}&page=${page - 1}&keyword=${encodeURIComponent(keyword)}${excludeParam}`,
        (res) => {
          let data;
          if (type === 'user') {
            data = res.data.content.map((e) => {
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
          } else {
            data = res.data.content
              .filter((e) => e.memberCount > 0)
              .map((e) => ({
                id: e.id,
                name: e.name,
                memberCount: e.memberCount,
                description: e.description,
                type: "group",
              }));
          }
          resolve(data);
        },
        {
          onError: handleError,
        }
      );
    });
  };

  function searchUsers({keyword, excludeUsers, pageSize, page}) {
    return searchEntities('user', {keyword, excludeIds: excludeUsers, pageSize, page});
  }

  function searchGroups({keyword, excludeGroups, pageSize, page}) {
    return searchEntities('group', {keyword, excludeIds: excludeGroups, pageSize, page});
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

    delayedSearch({keyword, excludeUsers, excludeGroups}, (results) => {
      if (active) {
        setOptions(results);
      }
    });

    return () => {
      active = false;
    };
  }, [value, keyword, delayedSearch]);

  return (
    <ProgrammingContestLayout
      title={t("common:problemSharing")}
      onBack={handleBack}>
      <Stack spacing={2} alignItems={"flex-start"} sx={{mt: 1}}>
        <Autocomplete
          id="add-members"
          multiple
          fullWidth
          size="small"
          PopperComponent={StyledAutocompletePopper}
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
          renderOption={(props, option, {selected}) => (
            <ListItem
              {...props}
              key={option.id}
              sx={{p: 0, color: selected && option.type === "group" ? "#1976d2" : "inherit"}}
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
                  <Avatar sx={{bgcolor: "#1976d2"}}>
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
        <Stack direction={"row"} spacing={2} alignItems={"center"}>
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
          <PrimaryButton
            disabled={value.length === 0 || isLoading}
            className={classes.btn}
            onClick={onAddMembers}
            startIcon={<Share/>}
          >
            {isLoading ? t("common:sharing") : t("common:share")}
          </PrimaryButton>
        </Stack>
      </Stack>

      <Divider sx={{margin: "24px 0 16px"}}/>
      <StandardTable
        columns={columnUserRoles}
        data={userRoles}
        hideCommandBar
        options={{
          selection: false,
          pageSize: 10,
          search: true,
          sorting: true,
        }}
        components={{
          Container: (props) => <Paper {...props} elevation={0}/>,
        }}
      />
    </ProgrammingContestLayout>
  );
}

const screenName = "SCR_USER_CONTEST_PROBLEM_ROLE";
export default withScreenSecurity(UserContestProblemRole, screenName, true);