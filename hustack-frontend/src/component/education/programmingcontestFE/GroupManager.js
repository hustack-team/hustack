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
} from "@mui/material";
import EditIcon from "@mui/icons-material/Edit";
import { request } from "api";
import { errorNoti } from "utils/notification";
import ProgrammingContestLayout from "./ProgrammingContestLayout";
import PrimaryButton from "../../button/PrimaryButton";
import StandardTable from "component/table/StandardTable";
import RichTextEditor from "../../common/editor/RichTextEditor";
import withScreenSecurity from "../../withScreenSecurity";
import { toFormattedDateTime } from "utils/dateutils";

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
          {t("generalInfo")}
        </Typography>
        <Stack direction="row" spacing={2}>
          <PrimaryButton
            onClick={() => {
              history.push(`/programming-contest/edit-group/${groupId}`);
            }}
            startIcon={<EditIcon />}
          >
            {t("common:edit", { name: "" })}
          </PrimaryButton>
        </Stack>
      </Stack>

      {loading && <LinearProgress />}
      <Grid container spacing={2} display={loading ? "none" : ""}>
        {[
          [t("groupName"), groupDetail.name],
          [
            t("status"),
            getStatuses(t).find((item) => item.value === groupDetail.status)?.label,
          ],
          [t("createdBy"), groupDetail.createdBy],
          [
            t("common:lastModifiedDate"),
            groupDetail.lastModifiedDate ? toFormattedDateTime(groupDetail.lastModifiedDate) : "N/A",
          ],
        ].map(([key, value, sx, helpText]) => (
          <Grid item xs={12} sm={12} md={3} key={key}>
            <Typography variant="subtitle2" color="textSecondary">
              {key}
            </Typography>
            <Typography variant="body1">{value || "N/A"}</Typography>
          </Grid>
        ))}
      </Grid>

      <Box sx={{ marginTop: "24px", marginBottom: "24px" }}>
        <Typography variant="h6" sx={{ marginBottom: "8px" }}>
          {t("description")}
        </Typography>
        <RichTextEditor
          toolbarHidden
          content={groupDetail.description}
          readOnly
          editorStyle={{ editor: {} }}
        />
      </Box>

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