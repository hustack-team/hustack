import AddIcon from "@material-ui/icons/Add";
import DeleteIcon from "@material-ui/icons/Delete";
import { Paper } from "@mui/material";
import React, { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { request } from "../../../api";
import { toFormattedDateTime } from "../../../utils/dateutils";
import StandardTable from "../../table/StandardTable";
import { useTranslation } from "react-i18next";
import { ConfirmDeleteDialog } from "component/dialog/ConfirmDeleteDialog";
import { errorNoti, successNoti } from "utils/notification";

export function ListTeacherGroupByMembership() {
  const [groups, setGroups] = useState([]);
  const [loading, setLoading] = useState(true);
  const [openDeleteDialog, setOpenDeleteDialog] = useState(false);
  const [selectedGroup, setSelectedGroup] = useState(null);
  const { t } = useTranslation(["education/programmingcontest/group", "common"]);

  const handleOpenDeleteDialog = (group) => {
    setSelectedGroup(group);
    setOpenDeleteDialog(true);
  };

  const handleCloseDeleteDialog = () => {
    setOpenDeleteDialog(false);
    setSelectedGroup(null);
  };

  const handleDeleteGroup = () => {
    if (selectedGroup) {
      request(
        "delete",
        `/members/groups/${selectedGroup.id}`,
        () => {
          setGroups(groups.filter((group) => group.id !== selectedGroup.id));
          successNoti(t("common:deleteSuccess"), 3000);
        },
        {
          403: () => errorNoti(t("common:noPermission"), 3000),
          404: () => errorNoti(t("common:groupNotFound"), 3000),
        }
      ).then(() => {
        handleCloseDeleteDialog();
      }).catch((err) => {
        errorNoti(t("common:deleteFailed"), 3000);
        handleCloseDeleteDialog();
      });
    }
  };

  const columns = [
    {
      title: t("common:groupName"),
      field: "name",
      render: (rowData) => (
        <Link
          to={{
            pathname: `/programming-contest/group-manager/${rowData.id}`,
          }}
        >
          {rowData.name}
        </Link>
      ),
    },
    {
      title: t("common:status"),
      field: "status",
    },
    {
      title: t("common:description"),
      field: "description",
    },
    {
      title: t("common:createdBy"),
      field: "createdByUserId",
    },
    {
      title: t("common:createdTime"),
      field: "lastUpdatedStamp",
      render: (rowData) => toFormattedDateTime(rowData.lastUpdatedStamp),
    },
    {
      title: t("common:delete"),
      render: (rowData) => (
        <DeleteIcon
          style={{ cursor: "pointer", color: "#f44336" }}
          onClick={() => handleOpenDeleteDialog(rowData)}
        />
      ),
    },
  ];

  function getGroupListByUserMembership() {
    request("get", "/members/groups", (res) => {
      setGroups(res.data);
    }).then(() => setLoading(false));
  }

  useEffect(() => {
    getGroupListByUserMembership();
  }, []);

  return (
    <>
      <StandardTable
        columns={columns}
        data={groups}
        hideCommandBar
        options={{
          pageSize: 5,
          selection: false,
          search: true,
          sorting: true,
        }}
        components={{
          Container: (props) => <Paper {...props} elevation={0} />,
        }}
        actions={[
          {
            icon: () => <AddIcon fontSize="large" />,
            tooltip: t("createNewGroup"),
            isFreeAction: true,
            onClick: () => {
              window.open("/programming-contest/create-group");
            },
          },
        ]}
      />
      <ConfirmDeleteDialog
        open={openDeleteDialog}
        handleClose={handleCloseDeleteDialog}
        handleDelete={handleDeleteGroup}
        entity={t("education/programmingcontest/group:group")}
        name={selectedGroup?.name}
      />
    </>
  );
}