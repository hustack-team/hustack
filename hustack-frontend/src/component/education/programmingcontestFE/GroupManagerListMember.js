import React, { useState, useEffect } from "react";
import { AppBar, Toolbar, Button, Dialog, DialogTitle, DialogContent, DialogActions, FormControl, InputLabel, Select, MenuItem } from "@mui/material";
import Box from "@mui/material/Box";
import SearchIcon from "@mui/icons-material/Search";
import { InputBase } from "@mui/material";
import { Search, SearchIconWrapper } from "./lib";
import StandardTable from "component/table/StandardTable";
import { request } from "api";
import { errorNoti, successNoti } from "utils/notification";

function AddMemberToGroupDialog({ open, onClose, onAddMember, groupId }) {
  const [selectedUserId, setSelectedUserId] = useState(null);
  const [selectedRole, setSelectedRole] = useState("PARTICIPANT");
  const [searchUsers, setSearchUsers] = useState([]);
  const [keyword, setKeyword] = useState("");
  const [pageSearchSize] = useState(10);
  const roles = ["PARTICIPANT", "MANAGER"]; // Loại bỏ OWNER

  const columns = [
    { title: "Index", field: "index" },
    { title: "UserID", field: "userName" },
    { title: "Full Name", field: "fullName" },
    {
      title: "Action",
      render: (row) => (
        <Button
          variant="contained"
          onClick={() => setSelectedUserId(row["userName"])}
          disabled={selectedUserId === row["userName"]}
        >
          Select
        </Button>
      ),
    },
  ];

  function searchUser(keyword, size, page) {
    request(
      "get",
      `/users?size=${size}&page=${page - 1}&keyword=${keyword}`,
      (res) => {
        const data = res.data.content.map((e, index) => ({
          index: index + 1,
          userName: e.userLoginId,
          fullName: (e.lastName ? e.lastName : "") + " " + (e.firstName ? e.firstName : ""),
        }));
        setSearchUsers(data);
      }
    ).then();
  }

  useEffect(() => {
    if (keyword.trim()) {
      searchUser(keyword, pageSearchSize, 1);
    } else {
      setSearchUsers([]);
    }
  }, [keyword]);

  const handleAdd = () => {
    if (selectedUserId) {
      onAddMember(groupId, selectedUserId, selectedRole);
      onClose();
    } else {
      errorNoti("Please select a user", 3000);
    }
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle>Add Member to Group</DialogTitle>
      <DialogContent>
        <Box sx={{ flexGrow: 1, marginBottom: 2 }}>
          <AppBar position="static" color={"transparent"}>
            <Toolbar>
              <Search>
                <SearchIconWrapper>
                  <SearchIcon />
                </SearchIconWrapper>
                <InputBase
                  style={{ paddingLeft: 50 }}
                  placeholder={"Search users..."}
                  value={keyword}
                  onChange={(event) => setKeyword(event.target.value)}
                />
              </Search>
            </Toolbar>
          </AppBar>
        </Box>
        <StandardTable
          title={"Users"}
          columns={columns}
          data={searchUsers}
          hideCommandBar
          options={{
            selection: false,
            pageSize: 10,
            search: false,
            sorting: true,
          }}
        />
        <FormControl fullWidth sx={{ marginTop: 2 }}>
          <InputLabel id="role-label">Role</InputLabel>
          <Select
            labelId="role-label"
            value={selectedRole}
            label="Role"
            onChange={(event) => setSelectedRole(event.target.value)}
          >
            {roles.map((role) => (
              <MenuItem key={role} value={role}>
                {role}
              </MenuItem>
            ))}
          </Select>
        </FormControl>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button onClick={handleAdd} variant="contained">Add</Button>
      </DialogActions>
    </Dialog>
  );
}

function GroupManagerListMember({ groupId, screenAuthorization }) {
  const [members, setMembers] = useState([]);
  const [openAddMemberDialog, setOpenAddMemberDialog] = useState(false);

  const columns = [
    { title: "UserID", field: "userId" },
    { title: "Role", field: "role" },
    {
      title: "Action",
      render: (row) => (
        <Button
          variant="contained"
          color="error"
          onClick={() => handleRemoveMember(row["userId"])}
          disabled={row["role"] === "OWNER"} // Không cho xóa OWNER
        >
          Remove
        </Button>
      ),
    },
  ];

  function getMembers() {
    request(
      "get",
      `/members/groups/${groupId}/members`,
      (res) => {
        setMembers(res.data);
      }
    ).then();
  }

  function handleAddMember(groupId, userId, role) {
    const body = {
      userId: userId,
      role: role,
    };
    request(
      "post",
      `/members/groups/${groupId}/members`,
      (res) => {
        successNoti("Member added successfully");
        getMembers(); // Làm mới danh sách
      },
      {
        onError: (err) => {
          errorNoti(err?.response?.data?.message || "Failed to add member", 3000);
        },
      },
      body
    ).then();
  }

  function handleRemoveMember(userId) {
    request(
      "delete",
      `/members/groups/${groupId}/members/${userId}`,
      (res) => {
        successNoti("Member removed successfully");
        getMembers(); // Làm mới danh sách
      },
      {
        onError: (err) => {
          errorNoti(err?.response?.data?.message || "Failed to remove member", 3000);
        },
      }
    ).then();
  }

  useEffect(() => {
    getMembers();
  }, [groupId]);

  return (
    <div>
      <Box sx={{ marginBottom: 2 }}>
        <Button
          variant="contained"
          onClick={() => setOpenAddMemberDialog(true)}
        >
          Add Member
        </Button>
      </Box>
      <StandardTable
        title={"Group Members"}
        columns={columns}
        data={members}
        hideCommandBar
        options={{
          selection: false,
          pageSize: 10,
          search: false,
          sorting: true,
        }}
      />
      <AddMemberToGroupDialog
        open={openAddMemberDialog}
        onClose={() => setOpenAddMemberDialog(false)}
        onAddMember={handleAddMember}
        groupId={groupId}
      />
    </div>
  );
}

export default GroupManagerListMember;