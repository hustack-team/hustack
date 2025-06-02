import React, { useState, useEffect } from "react";
import { useHistory, useParams } from "react-router-dom";
import { useTranslation } from "react-i18next";
import {
  Box,
  Grid,
  LinearProgress,
  Stack,
  Typography,
  Avatar,
  ListItemAvatar,
  ListItemText,
  IconButton,
  Tooltip,
} from "@mui/material";
import EditIcon from "@mui/icons-material/Edit";
import HelpOutlineIcon from "@mui/icons-material/HelpOutline";
import { request } from "api";
import { errorNoti } from "utils/notification";
import ProgrammingContestLayout from "./ProgrammingContestLayout";
import PrimaryButton from "../../button/PrimaryButton";
import StandardTable from "component/table/StandardTable";
import withScreenSecurity from "../../withScreenSecurity";

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

const getStatuses = (t) => [
  { label: t("common:statusActive"), value: "ACTIVE" },
  { label: t("common:statusInactive"), value: "INACTIVE" },
];

function detail(key, value, sx, helpText) {
  return (
    <Stack>
      <Typography variant="subtitle2" sx={{ fontWeight: 600, ...sx?.key }}>
        {helpText ? (
          <>
            {key}
            <Tooltip arrow title={helpText}>
              <IconButton sx={{ p: 0.5, pt: 0 }}>
                <HelpOutlineIcon sx={{ fontSize: 16, color: "#000000de" }} />
              </IconButton>
            </Tooltip>
          </>
        ) : (
          key
        )}
      </Typography>
      <Typography>{value || "-"}</Typography>
      {helpText && (
        <Typography variant="caption" color="error">
          {helpText}
        </Typography>
      )}
    </Stack>
  );
}

function GroupManager({ screenAuthorization }) {
  const { groupId } = useParams();
  const history = useHistory();
  const { t } = useTranslation(["common", "validation"]);

  const [groupDetail, setGroupDetail] = useState({
    id: "",
    name: "",
    status: "",
    description: "",
    createdBy: "",
    lastModifiedDate: "",
  });
  const [members, setMembers] = useState([]);
  const [loading, setLoading] = useState(true);

  const statuses = getStatuses(t);

  const handleExit = () => {
    history.push("/programming-contest/teacher-list-group");
  };

  const fetchGroupDetails = () => {
    request(
      "get",
      `/groups/${groupId}`,
      (res) => {
        const data = res.data;
        setGroupDetail({
          id: data.id,
          name: data.name,
          status: data.status,
          description: data.description || "",
          createdBy: data.createdBy,
          lastModifiedDate: data.lastModifiedDate,
        });
        setLoading(false);
      },
      {
        onError: (err) => {
          errorNoti("Failed to fetch group details", 3000);
          console.error("Error fetching group details:", err);
          setLoading(false);
        },
      }
    );
  };

  const fetchMembers = () => {
    request(
      "get",
      `/groups/${groupId}/members`,
      (res) => {
        const membersData = res.data.map((member) => ({
          userId: member.userId,
          fullName: member.fullName || "Anonymous",
        }));
        setMembers(membersData);
      },
      {
        onError: (err) => {
          if (err.response?.status === 404) {
            errorNoti("Group not found", 3000);
          } else if (err.response?.status === 403) {
            errorNoti("You are not authorized to view group members", 3000);
          } else {
            errorNoti("Failed to fetch members", 3000);
          }
          console.error("Error fetching members:", err);
        },
      }
    );
  };

  const columns = [
    {
      title: t("common:member"),
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
  ];

  useEffect(() => {
    fetchGroupDetails();
    fetchMembers();
  }, [groupId]);

  return (
    <ProgrammingContestLayout title={t("viewGroup")} onBack={handleExit}>
      <Stack direction="row" spacing={2} mb={1.5} justifyContent="space-between">
        <Typography variant="h6" component="span">
          {t("common:generalInfo")}
        </Typography>
        <Stack direction="row" spacing={2}>
          <PrimaryButton
            onClick={() => {
              history.push(`/programming-contest/group-form/${groupId}`);
            }}
            startIcon={<EditIcon />}
          >
            {t("common:edit", { name: "" })}
          </PrimaryButton>
        </Stack>
      </Stack>

      {loading && <LinearProgress />}
      
      <Grid container spacing={2} display={loading ? "none" : ""}>
        <Grid item xs={12} sm={12} md={6}>
          <Stack spacing={2}>
            {[
              [t("groupName"), groupDetail.name],
              [t("status"), statuses.find(item => item.value === groupDetail.status)?.label],
            ].map(([key, value, sx, helpText]) => (
              <div key={key}>
                {detail(key, value, sx, helpText)}
              </div>
            ))}
          </Stack>
        </Grid>
        <Grid item xs={12} sm={12} md={6}>
          {detail(t("description"), groupDetail.description || "-")}
        </Grid>
      </Grid>

      <Box sx={{ marginTop: "24px" }}>
        <StandardTable
          title={t("common:groupMember")}
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
      </Box>
    </ProgrammingContestLayout>
  );
}

const screenName = "SCR_GROUP_MANAGER";
export default withScreenSecurity(GroupManager, screenName, true);