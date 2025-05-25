import React, { useState, useEffect } from "react";
import { Grid, LinearProgress, Typography } from "@mui/material";
import { LoadingButton } from "@mui/lab";
import TextField from "@mui/material/TextField";
import { request } from "api";
import { errorNoti, successNoti } from "utils/notification";
import HustContainerCard from "component/common/HustContainerCard";
import StyledSelect from "component/select/StyledSelect";
import { useTranslation } from "react-i18next";
import { useParams } from "react-router-dom";
import withScreenSecurity from "component/withScreenSecurity";
import { toFormattedDateTime } from "utils/dateutils";

function EditGroup() {
  const { t } = useTranslation(["common", "validation"]);
  const { groupId } = useParams();
  const [loading, setLoading] = useState(true);
  const [groupDetails, setGroupDetails] = useState({
    id: "",
    name: "",
    status: "",
    description: "",
    createdByUserId: "",
    lastUpdatedStamp: "",
  });
  const [error, setError] = useState(null);

  console.log("groupId from useParams:", groupId); // Debug log

  const statusOptions = [
    { label: "Active", value: "ACTIVE" },
    { label: "Inactive", value: "INACTIVE" },
  ];

  // Fetch group details
  useEffect(() => {
    if (!groupId) {
      setError(t("validation:invalidGroupId", { ns: "common" }));
      setLoading(false);
      errorNoti(t("validation:invalidGroupId", { ns: "common" }), 3000);
      return;
    }

    setLoading(true);
    request(
      "get",
      `/members/groups/${groupId}`,
      (res) => {
        setGroupDetails({
          id: res.data.id || "",
          name: res.data.name || "",
          status: res.data.status || "",
          description: res.data.description || "",
          createdByUserId: res.data.createdByUserId || "",
          lastUpdatedStamp: res.data.lastUpdatedStamp || "",
        });
        setLoading(false);
      },
      (error) => {
        errorNoti(t("error", { ns: "common" }), 3000);
        console.error("Error fetching group details:", error);
        setLoading(false);
      }
    );
  }, [groupId, t]);

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setGroupDetails((prev) => ({
      ...prev,
      [name]: value,
    }));
  };

  const handleSubmit = () => {
    if (!groupId) {
      errorNoti(t("validation:invalidGroupId", { ns: "common" }), 3000);
      return;
    }

    if (!groupDetails.name.trim()) {
      errorNoti(t("validation:required", { field: "Group Name" }), 3000);
      return;
    }

    setLoading(true);
    const body = {
      name: groupDetails.name,
      status: groupDetails.status,
      description: groupDetails.description,
    };

    request(
      "put",
      `/members/groups/${groupId}`,
      (res) => {
        successNoti("Group updated successfully", 3000);
        setGroupDetails({
          id: res.data.id || "",
          name: res.data.name || "",
          status: res.data.status || "",
          description: res.data.description || "",
          createdByUserId: res.data.createdByUserId || "",
          lastUpdatedStamp: res.data.lastUpdatedStamp || "",
        });
        setLoading(false);
      },
      (error) => {
        errorNoti(t("error", { ns: "common" }), 3000);
        console.error("Error updating group details:", error);
        setLoading(false);
      },
      body
    );
  };

  if (error) {
    return (
      <HustContainerCard title="Edit Group">
        <Typography color="error" sx={{ p: 2 }}>
          {error}
        </Typography>
      </HustContainerCard>
    );
  }

  return (
    <div>
      <HustContainerCard title={groupDetails.name || "Edit Group"}>
        {loading ? (
          <LinearProgress />
        ) : (
          <>
            <Grid
              container
              rowSpacing={3}
              spacing={2}
              display={loading ? "none" : ""}
            >
              <Grid item sm={12} md={4}>
                <TextField
                  required
                  fullWidth
                  size="small"
                  id="name"
                  label="Group Name"
                  name="name"
                  value={groupDetails.name}
                  onChange={handleInputChange}
                />
              </Grid>
              <Grid item sm={12} md={4}>
                <StyledSelect
                  fullWidth
                  id="status"
                  label="Status"
                  name="status"
                  value={groupDetails.status}
                  options={statusOptions}
                  onChange={handleInputChange}
                />
              </Grid>

              {/* Second row - Description (full width) */}
              <Grid item xs={12}>
                <TextField
                  fullWidth
                  size="small"
                  id="description"
                  label="Description"
                  name="description"
                  value={groupDetails.description}
                  onChange={handleInputChange}
                  multiline
                  rows={4}
                />
              </Grid>

              {/* Third row - Created By and Last Updated */}
              <Grid item sm={12} md={6}>
                <TextField
                  fullWidth
                  size="small"
                  id="createdByUserId"
                  label="Created By"
                  value={groupDetails.createdByUserId}
                  disabled
                />
              </Grid>
              <Grid item sm={12} md={6}>
                <TextField
                  fullWidth
                  size="small"
                  id="lastUpdatedStamp"
                  label="Last Updated"
                  value={toFormattedDateTime(groupDetails.lastUpdatedStamp)}
                  disabled
                />
              </Grid>
            </Grid>
            <LoadingButton
              loading={loading}
              variant="contained"
              sx={{ textTransform: "none", mt: 4 }}
              onClick={handleSubmit}
              disabled={!groupDetails.name.trim() || !groupId}
            >
              Save
            </LoadingButton>
          </>
        )}
      </HustContainerCard>
    </div>
  );
}

const screenName = "SCR_EDIT_GROUP";
export default withScreenSecurity(EditGroup, screenName, true);