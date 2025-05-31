//import { makeStyles } from "@mui/styles"; //"@material-ui/core/styles";
import {makeStyles} from "@material-ui/core/styles";
import {
  Autocomplete,
  Avatar,
  ListItem,
  ListItemAvatar,
  ListItemText,
  Popper,
  Stack,
  TextField,
  Tooltip,
} from "@mui/material";
import {autocompleteClasses} from "@mui/material/Autocomplete";
import {styled} from "@mui/material/styles";
import {debounce} from "@mui/material/utils";
import {request} from "api";
import PrimaryButton from "component/button/PrimaryButton";
import StyledSelect from "component/select/StyledSelect";
import {getTextAvatar} from "layout/account/AccountButton";
import {isEmpty, trim} from "lodash";
import {useEffect, useMemo, useState} from "react";
import {successNoti} from "utils/notification";
import UploadUserToContestDialog from "./UploadUserToContestDialog";
import UploadUserUpdateFullNameContestDialog from "./UploadUserUpdateFullNameContestDialog";
import { t } from "i18next";


// https://mui.com/material-ui/react-avatar/#letter-avatars
function stringToColor(string) {
  if (!string) return "#000";
  let hash = 0;
  let i;

  /* eslint-disable no-bitwise */
  for (i = 0; i < string.length; i += 1) {
    hash = string.charCodeAt(i) + ((hash << 5) - hash);
  }

  let color = "#";

  for (i = 0; i < 3; i += 1) {
    const value = (hash >> (i * 8)) & 0xff;
    color += `00${value.toString(16)}`.slice(-2);
  }
  /* eslint-enable no-bitwise */

  return color;
}

export function stringAvatar(id, name) {
  return {
    children: getTextAvatar(name)?.toLocaleUpperCase(),
    sx: {
      bgcolor: stringToColor(id),
    },
  };
}

const StyledAutocompletePopper = styled(Popper)(({ theme }) => ({
  [`& .${autocompleteClasses.paper}`]: {
    boxShadow:
      "0 12px 28px 0 rgba(0, 0, 0, 0.2), 0 2px 4px 0 rgba(0, 0, 0, 0.1), inset 0 0 0 1px rgba(255, 255, 255, 0.5)",
    margin: 0,
    padding: 8,
    borderRadius: 8,
    // backgroundColor: "black",
  },
  [`& .${autocompleteClasses.listbox}`]: {
    padding: 0,
    // backgroundColor: "orange",
    [`& .${autocompleteClasses.option}`]: {
      padding: "0px 8px",
      borderRadius: 8,
      // backgroundColor: "red",
      "&:hover": {
        backgroundColor: "#eeeeee",
      },
    },
  },
}));

export function PopperComponent(props) {
  // console.log(props);
  // const { disablePortal, anchorEl, open, ...other } = props;
  return <StyledAutocompletePopper {...props} />;
}

const useStyles = makeStyles((theme) => ({
  dialogContent: {
    minWidth: 480,
    minHeight: 64,
  },
  btn: { margin: "4px 8px" },
}));

const roles = [
  {
    label: "Participant",
    value: "PARTICIPANT",
  },
  {
    label: "Manager",
    value: "MANAGER",
  },
  {
    label: "Owner",
    value: "OWNER",
  },
];

const defaultPageSize = 10;

export default function AddMember2Contest(props) {
  const contestId = props.contestId;

  const classes = useStyles();
  const [selectedRole, setSelectedRole] = useState(roles[0].value);

  //
  const [openUploadDialog, setOpenUploadDialog] = useState(false);
  const [openUploadToUpdateUserFullnameDialog, setOpenUploadToUpdateUserFullnameDialog] = useState(false);

  //
  const [value, setValue] = useState([]);
  const [options, setOptions] = useState([]);
  // const [roles, setRoles] = useState([]);
  const [keyword, setKeyword] = useState("");

  const delayedSearch = useMemo(
    () =>
      debounce(({ keyword, excludeUsers, excludeGroups }, callback) => {
        Promise.all([
          searchUsers({ keyword, excludeUsers, pageSize: defaultPageSize, page: 1 }),
          searchGroups({ keyword, excludeGroups, pageSize: defaultPageSize, page: 1 }),
        ]).then(([userResults, groupResults]) => {
          callback([...userResults, ...groupResults]);
        });
      }, 400),
    []
  );

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
          const data = res.data.content.map((e) => ({
            id: e.id,
            name: e.name,
            type: "group",
          }));
          resolve(data);
        }
      );
    });
  }

  function onAddMembers() {
    const userIds = value.filter((item) => item.type === "user").map((user) => user.id);
    const groupIds = value.filter((item) => item.type === "group").map((group) => group.id);

    let body = {
      userIds,
      groupIds,
      role: selectedRole,
    };

    request(
      "post",
      `/contests/${contestId}/users`,
      (res) => {
        successNoti("Users and groups were successfully added", 3000);
        props.onAddedSuccessfully();
        setValue([]); // Clear selections
      },
      {},
      body
    );
  }

  useEffect(() => {
    let active = true;

    // if (keyword === "" && value) {
    //   setOptions(value ? [value] : []);
    //   return undefined;
    // }

    // console.log(value);
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
    <>
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
            // console.log("getOptionLabel with option", option);
            option.type === "group" ? `Group: ${option.name}` : option.name
          }
          filterOptions={(x) => x} // disable filtering on client
          options={options}
          // autoComplete
          // includeInputInList
          // filterSelectedOptions
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
                  <Avatar
                    sx={{ bgcolor: "#1976d2" }}
                  >
                    G
                  </Avatar>
                </ListItemAvatar>
              )}
              <ListItemText
                primary={option.type === "group" ? `Group: ${option.name}` : option.name}
                secondary={option.id}
              />
            </ListItem>
          )}
        />
        <StyledSelect
          required
          key={"Role"}
          label="Role"
          options={roles}
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
            Add
          </PrimaryButton>
          <Tooltip arrow title="Add members by uploading Excel file">
            <PrimaryButton
              onClick={() => {
                setOpenUploadDialog(true);
              }}
            >
              Import
            </PrimaryButton>
          </Tooltip>
          <Tooltip arrow title="Add members by uploading Excel file">
            <PrimaryButton
              onClick={() => {
                setOpenUploadToUpdateUserFullnameDialog(true);
              }}
            >
              Import update fullname
            </PrimaryButton>
          </Tooltip>
        </Stack>
      </Stack>
      <UploadUserToContestDialog
        isOpen={openUploadDialog}
        contestId={contestId}
        onClose={() => {
          setOpenUploadDialog(false);
          props.onAddedSuccessfully();
        }}
      />
      <UploadUserUpdateFullNameContestDialog
        isOpen={openUploadToUpdateUserFullnameDialog}
        contestId={contestId}
        onClose={() => {
          setOpenUploadToUpdateUserFullnameDialog(false);
          props.onAddedSuccessfully();
        }}
      />
    </>
  );
}