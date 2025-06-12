import AddIcon from "@material-ui/icons/Add";
import {Paper} from "@mui/material";
import React, {useEffect, useState} from "react";
import {Link} from "react-router-dom";
import {request} from "../../../api";
import {toFormattedDateTime} from "../../../utils/dateutils";
import StandardTable from "../../table/StandardTable";
import {useTranslation} from "react-i18next";
import {getContestStatuses} from "./EditContest";

export function ListContestManagerByRegistration() {
  const [contests, setContests] = useState([]);
  const [loading, setLoading] = useState(true);
  const {t} = useTranslation(["education/programmingcontest/contest", "education/programmingcontest/problem", 'common']);
  const contestStatuses = getContestStatuses(t)

  const columns = [
    {
      title: t("contestName"),
      field: "contestName",
      render: (rowData) => (
        <Link
          to={{
            pathname:
              "/programming-contest/contest-manager/" + rowData["contestId"],
          }}
        >
          {rowData["contestName"]}
        </Link>
      ),
    },
    {
      title: t("common:status"),
      field: "statusId",
      render: (rowData) => `${contestStatuses.find(item => item.value === rowData.statusId)?.label || ""}`
    },
    {
      title: "Role",
      field: "roleId"
    },
      {
      title: t("common:createdBy"),
      field: "userId"
    },
    // {
    //   title: t('common:manager'),
    //   field: "userId"
    // },
    {
      title: t("common:createdTime"),
      field: "startAt",
      render: (rowData) => toFormattedDateTime(rowData["startAt"]),
    },
  ];

  function getContestListByUserRole() {
    request("get", "/contests", (res) => {
      setContests(res.data);
    }).then(() => setLoading(false));
  }

  useEffect(() => {
    getContestListByUserRole();
  }, []);

  return (
    // <>
    //   <Stack direction="row" justifyContent='space-between'>
    //     <Typography variant="h6" sx={{mb: 1.5}}>My Contests</Typography>
    //   </Stack>
      <StandardTable
        columns={columns}
        data={contests}
        hideCommandBar
        options={{
          pageSize: 5,
          selection: false,
          search: true,
          sorting: true,
        }}
        components={{
          Container: (props) => <Paper {...props} elevation={0}/>,
        }}
        actions={[
          {
            icon: () => {
              return <AddIcon fontSize="large"/>;
            },
            tooltip: "Create new Contest",
            isFreeAction: true,
            onClick: () => {
              window.open("/programming-contest/create-contest");
            },
          },
        ]}
      />
    // </>
  );
}
