import AddIcon from "@material-ui/icons/Add";
import { Paper } from "@mui/material";
import React, { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { request } from "../../../api";
import { toFormattedDateTime } from "../../../utils/dateutils";
import StandardTable from "../../table/StandardTable";
import { useTranslation } from "react-i18next";

export function ListTeacherGroupByMembership() {
  const [groups, setGroups] = useState([]);
  const [loading, setLoading] = useState(true);
  const { t } = useTranslation(["education/programmingcontest/group", "common"]);

  const columns = [
    {
      title: t("groupName"),
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
      title: t("role"),
      field: "role",
    },
    {
      title: t("common:createdBy"),
      field: "createdByUserId",
    },
    {
      title: t("common:createdTime"),
      field: "createdStamp",
      render: (rowData) => toFormattedDateTime(rowData.createdStamp),
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
  );
}