import React, { useState } from "react";
import { MuiPickersUtilsProvider } from "@material-ui/pickers";
import DateFnsUtils from "@date-io/date-fns";
import { LoadingButton } from "@mui/lab";
import { Grid } from "@mui/material";
import Box from "@mui/material/Box";
import TextField from "@mui/material/TextField";
import MenuItem from "@mui/material/MenuItem";
import { request } from "api";
import withScreenSecurity from "component/withScreenSecurity";
import { useHistory } from "react-router-dom";
import { errorNoti, successNoti } from "utils/notification";
import HustContainerCard from "../../common/HustContainerCard";
import { sleep } from "./lib";

function CreateGroup(props) {
  const history = useHistory();

  const [groupName, setGroupName] = useState("");
  const [status, setStatus] = useState("ACTIVE");
  const [description, setDescription] = useState("");
  const [loading, setLoading] = useState(false);

  const isValidGroupName = () => {
    return new RegExp(/[%^/\\|.?;[\]]/g).test(groupName);
  };

  function handleSubmit() {
    setLoading(true);
    let body = {
      name: groupName,
      status: status,
      description: description,
      userIds: [],
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