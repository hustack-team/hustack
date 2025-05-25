import React, { useState, useEffect } from "react";
import { Grid, LinearProgress } from "@mui/material";
import EditIcon from "@mui/icons-material/Edit";
import { request } from "api";
import { errorNoti } from "utils/notification";
import PrimaryButton from "component/button/PrimaryButton";
import HustContainerCard from "component/common/HustContainerCard";
import { useHistory } from "react-router-dom";
import { detail } from "./ContestProblemSubmissionDetailViewedByManager";
import { toFormattedDateTime } from "utils/dateutils";

function GroupManagerDetail({ groupId }) {
  const history = useHistory();
  const [groupDetails, setGroupDetails] = useState({
    id: "",
    name: "",
    status: "",
    description: "",
    createdByUserId: "",
    lastUpdatedStamp: "",
  });
  const [loading, setLoading] = useState(true);

  // Fetch group details
  useEffect(() => {
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
        errorNoti("Failed to fetch group details", 3000);
        console.error("Error fetching group details:", error);
        setLoading(false);
      }
    );
  }, [groupId]);

  const handleEdit = () => {
    history.push(`/programming-contest/group-edit/${groupId}`);
  };

  return (
    <HustContainerCard
      title={groupDetails.name}
      action={
        <PrimaryButton
          color="info"
          onClick={handleEdit}
          startIcon={<EditIcon />}
        >
          Edit
        </PrimaryButton>
      }
    >
      {loading && <LinearProgress />}
      <Grid container spacing={2} display={loading ? "none" : ""}>
        {[
          ["Name", groupDetails.name],
          ["Status", groupDetails.status],
          ["Description", groupDetails.description],
          ["Created By", groupDetails.createdByUserId],
          ["Last Updated", toFormattedDateTime(groupDetails.lastUpdatedStamp)],
        ].map(([key, value, sx, helpText]) => (
          <Grid item xs={12} sm={12} md={4} key={key}>
            {detail(key, value, sx, helpText)}
          </Grid>
        ))}
      </Grid>
    </HustContainerCard>
  );
}

export default GroupManagerDetail;