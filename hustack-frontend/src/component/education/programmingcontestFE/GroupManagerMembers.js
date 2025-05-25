import React, { useState, useMemo, useEffect } from "react";
import {
  Stack, Autocomplete, TextField, ListItem, ListItemAvatar, ListItemText, Avatar, Popper, IconButton
} from "@mui/material";
import DeleteIcon from "@mui/icons-material/Delete";
import { autocompleteClasses } from "@mui/material/Autocomplete";
import { styled } from "@mui/material/styles";
import { debounce } from "@mui/material/utils";
import { request } from "api";
import { successNoti, errorNoti } from "utils/notification";
import PrimaryButton from "component/button/PrimaryButton";
import StandardTable from "component/table/StandardTable";
import { isEmpty, trim } from "lodash";
import { toFormattedDateTime } from "utils/dateutils";

// Styled components for Autocomplete Popper
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

// Function to generate avatar color based on string
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

// Function to generate avatar properties
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

function GroupManagerMembers({ groupId, screenAuthorization, refresh }) {
  const [members, setMembers] = useState([]);
  const [value, setValue] = useState([]);
  const [options, setOptions] = useState([]);
  const [keyword, setKeyword] = useState("");
  const [loading, setLoading] = useState(false);

  const columns = [
    {
      title: "Member",
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
      title: "Added Time",
      field: "addedTime",
      render: (rowData) => toFormattedDateTime(rowData.addedTime),
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

  const delayedSearch = useMemo(
    () =>
      debounce(({ keyword, exclude }, callback) => {
        const url = keyword
          ? `/users?size=10&page=0&keyword=${keyword}${
              exclude ? exclude.map((user) => "&exclude=" + user.userName).join("") : ""
            }`
          : `/users?size=10&page=0${exclude ? exclude.map((user) => "&exclude=" + user.userName).join("") : ""}`;

        request(
          "get",
          url,
          (res) => {
            const data = res.data.content.map((e) => {
              const user = {
                userName: e.userLoginId,
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

  function getMembers() {
    request(
      "get",
      `/members/groups/${groupId}/members`,
      (res) => {
        const data = res.data.map((e) => ({
          userId: e.userId,
          fullName: e.fullName || "Anonymous",
          addedTime: e.addedTime,
        }));
        setMembers(data);
        setLoading(false);
      },
      (error) => {
        errorNoti("Failed to fetch members", 3000);
        console.error("Error fetching members:", error);
        setLoading(false);
      }
    ).then();
  }

  function handleAddMembers() {
    const userIds = value.map((user) => user.userName);
    if (!userIds.length) return;

    setLoading(true);
    request(
      "post",
      `/members/groups/${groupId}/members`,
      (res) => {
        successNoti("Users were successfully added to the group", 3000);
        setValue([]);
        setKeyword("");
        setOptions([]);
        getMembers(); // Refresh member list to include new addedTime
        setLoading(false);
      },
      (error) => {
        errorNoti("Failed to add users to the group", 3000);
        console.error("Error adding members:", error);
        setLoading(false);
      },
      userIds
    );
  }

  function handleRemoveMember(userId) {
    setLoading(true);
    request(
      "delete",
      `/members/groups/${groupId}/members/${userId}`,
      (res) => {
        successNoti("Member removed successfully", 3000);
        getMembers();
        setLoading(false);
      },
      {
        onError: (err) => {
          errorNoti(err?.response?.data?.message || "Failed to remove member", 3000);
          setLoading(false);
        },
      }
    ).then();
  }

  useEffect(() => {
    getMembers();
  }, [groupId, refresh]);

  useEffect(() => {
    const excludeIds = value;
    delayedSearch({ keyword, exclude: excludeIds }, (results) => {
      let newOptions = [];
      if (results) {
        newOptions = [...newOptions, ...results];
      }
      setOptions(newOptions);
    });
  }, [value, keyword, delayedSearch]);

  return (
    <Stack spacing={2} sx={{ p: 2 }}>
      <Stack spacing={2}>
        <Autocomplete
          id="add-group-members"
          multiple
          fullWidth
          size="small"
          PopperComponent={PopperComponent}
          getOptionLabel={(option) => option.fullName || ""}
          filterOptions={(x) => x}
          options={options}
          noOptionsText="No matches found"
          value={value}
          onChange={(event, newValue) => {
            setValue(newValue);
            setOptions(newValue ? [...newValue, ...options.filter((opt) => !newValue.some((v) => v.userName === opt.userName))] : options);
          }}
          onInputChange={(event, newInputValue) => {
            setKeyword(newInputValue);
          }}
          renderInput={(params) => (
            <TextField
              {...params}
              label="Add Members"
              placeholder="Search by ID or name"
              inputProps={{
                ...params.inputProps,
                autoComplete: "new-password",
              }}
              disabled={loading}
            />
          )}
          renderOption={(props, option) => (
            <ListItem {...props} key={option.userName} sx={{ p: 0 }}>
              <ListItemAvatar>
                <Avatar
                  alt="account avatar"
                  {...stringAvatar(option.userName, option.fullName)}
                />
              </ListItemAvatar>
              <ListItemText
                primary={option.fullName}
                secondary={option.userName}
              />
            </ListItem>
          )}
        />
        <PrimaryButton
          disabled={loading || !value.length}
          onClick={handleAddMembers}
          sx={{ alignSelf: "flex-start" }}
        >
          Add
        </PrimaryButton>
      </Stack>
      <StandardTable
        title="Group Members"
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
    </Stack>
  );
}

export default GroupManagerMembers;