import React, { useState, useEffect } from "react";
import { MuiPickersUtilsProvider } from "@material-ui/pickers";
import DateFnsUtils from "@date-io/date-fns";
import { LoadingButton } from "@mui/lab";
import { Grid, Button, Chip, AppBar, Toolbar, InputBase } from "@mui/material";
import Box from "@mui/material/Box";
import TextField from "@mui/material/TextField";
import MenuItem from "@mui/material/MenuItem";
import SearchIcon from "@mui/icons-material/Search";
import StandardTable from "component/table/StandardTable";
import { request } from "api";
import withScreenSecurity from "component/withScreenSecurity";
import { useHistory } from "react-router-dom";
import { errorNoti, successNoti } from "utils/notification";
import HustContainerCard from "../../common/HustContainerCard";
import { sleep } from "./lib";
import { Search, SearchIconWrapper } from "./lib";

function CreateGroup(props) {
  const history = useHistory();

  const [groupName, setGroupName] = useState("");
  const [status, setStatus] = useState("ACTIVE");
  const [description, setDescription] = useState("");
  const [loading, setLoading] = useState(false);
  const [keyword, setKeyword] = useState("");
  const [searchUsers, setSearchUsers] = useState([]);
  const [pageSearchSize] = useState(20);
  const [selectedUserIds, setSelectedUserIds] = useState([]);

  const isValidGroupName = () => {
    return new RegExp(/[%^/\\|.?;[\]]/g).test(groupName);
  };

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

  function handleSelectUser(userId) {
    if (!selectedUserIds.includes(userId)) {
      setSelectedUserIds([...selectedUserIds, userId]);
    }
  }

  function handleRemoveUser(userId) {
    setSelectedUserIds(selectedUserIds.filter(id => id !== userId));
  }

  function handleSubmit() {
    setLoading(true);
    let body = {
      name: groupName,
      status: status,
      description: description,
      userIds: selectedUserIds,
    };

    request(
      "post",
      "/members/groups",
      (res) => {
        successNoti("Group created successfully");
        sleep(1000).then(() => {
          history.push(`/programming-contest/group-manager/${res.data.id}`);
        });
      },
      {
        onError: (err) => {
          errorNoti(err?.response?.data?.message || "Failed to create group", 5000);
        },
      },
      body
    )
      .then()
      .finally(() => setLoading(false));
  }

  useEffect(() => {
    searchUser(keyword, pageSearchSize, 1);
  }, []);

  return (
    <div>
      <MuiPickersUtilsProvider utils={DateFnsUtils}>
        <HustContainerCard title={"Create Teacher Group"}>
          {!loading && (
            <Box>
              <Grid container rowSpacing={3} spacing={2}>
                <Grid item xs={6}>
                  <TextField
                    fullWidth
                    autoFocus
                    required
                    value={groupName}
                    id="groupName"
                    label="Group Name"
                    onChange={(event) => {
                      setGroupName(event.target.value);
                    }}
                    error={isValidGroupName()}
                    helperText={
                      isValidGroupName()
                        ? "Group Name must not contain special characters including %^/\\|.?;[]"
                        : ""
                    }
                  />
                </Grid>
                <Grid item xs={6}>
                  <TextField
                    fullWidth
                    select
                    required
                    value={status}
                    id="status"
                    label="Status"
                    onChange={(event) => {
                      setStatus(event.target.value);
                    }}
                  >
                    <MenuItem value="ACTIVE">Active</MenuItem>
                    <MenuItem value="INACTIVE">Inactive</MenuItem>
                  </TextField>
                </Grid>
                <Grid item xs={12}>
                  <TextField
                    fullWidth
                    multiline
                    rows={4}
                    value={description}
                    id="description"
                    label="Description"
                    onChange={(event) => {
                      setDescription(event.target.value);
                    }}
                  />
                </Grid>
                <Grid item xs={12}>
                  <Box sx={{ flexGrow: 1, marginBottom: 2 }}>
                    <AppBar position="static" color={"transparent"}>
                      <Toolbar>
                        <Search>
                          <SearchIconWrapper>
                            <SearchIcon />
                          </SearchIconWrapper>
                          <InputBase
                            style={{ paddingLeft: 50 }}
                            placeholder={"search..."}
                            onChange={(event) => {
                              setKeyword(event.target.value);
                              searchUser(event.target.value, pageSearchSize, 1);
                            }}
                          />
                        </Search>
                      </Toolbar>
                    </AppBar>
                  </Box>

                  <StandardTable
                    title={"Users"}
                    columns={[
                      { title: "Index", field: "index" },
                      { title: "UserID", field: "userName" },
                      { title: "Full Name", field: "fullName" },
                      {
                        title: "Action",
                        render: (row) => (
                          <Button
                            variant="contained"
                            onClick={() => handleSelectUser(row["userName"])}
                            disabled={selectedUserIds.includes(row["userName"])}
                          >
                            Select
                          </Button>
                        ),
                      },
                    ]}
                    data={searchUsers}
                    hideCommandBar
                    options={{
                      selection: false,
                      pageSize: 20,
                      search: false,
                      sorting: true,
                    }}
                  />

                  <Box sx={{ marginTop: 2, marginBottom: 2 }}>
                    <strong>Selected Users:</strong>
                    {selectedUserIds.length > 0 ? (
                      selectedUserIds.map((userId) => (
                        <Chip
                          key={userId}
                          label={userId}
                          onDelete={() => handleRemoveUser(userId)}
                          sx={{ margin: 0.5 }}
                        />
                      ))
                    ) : (
                      <span> No users selected</span>
                    )}
                  </Box>
                </Grid>
              </Grid>
            </Box>
          )}

          <LoadingButton
            loading={loading}
            variant="contained"
            style={{ marginTop: "36px" }}
            onClick={handleSubmit}
            disabled={isValidGroupName() || loading || !groupName.trim()}
          >
            Save
          </LoadingButton>
        </HustContainerCard>
      </MuiPickersUtilsProvider>
    </div>
  );
}

const screenName = "SCR_CREATE_TEACHER_GROUP";
export default withScreenSecurity(CreateGroup, screenName, true);