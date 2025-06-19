import {LinearProgress, Paper} from "@mui/material";
import {request} from "api";
import StandardTable from "component/table/StandardTable";
import React, {useEffect, useState} from "react";
import {Link} from "react-router-dom";
import {toFormattedDateTime} from "utils/dateutils";
import {a11yProps, AntTab, AntTabs, TabPanel} from "component/tab";
import {useTranslation} from "react-i18next";
import {getContestStatuses} from "./EditContest";

export default function ContestStudentList() {
  const [contests, setContests] = useState([]);
  const [publicContests, setPublicContests] = useState([]);
  const [loading, setLoading] = useState(false);
  const [selectedTab, setSelectedTab] = useState(0);
  const {t} = useTranslation(["common", 'education/programmingcontest/contest']);
  const contestStatuses = getContestStatuses(t)

  const registeredColumns = [
    {
      title: t('education/programmingcontest/contest:contest'),
      field: "contestName",
      render: (rowData) => (
        <Link
          to={{
            pathname:
              "/programming-contest/student-view-contest-detail/" +
              rowData["contestId"],
          }}
        >
          {rowData["contestName"]}
        </Link>
      ),
    },
    {
      title: t("common:status"),
      field: "status",
      render: (rowData) => `${contestStatuses.find(item => item.value === rowData.status)?.label || ""}`
    },
    {title: t('common:manager'), field: "createdBy"},
    // {title: "Created At", field: "createdAt"},
  ];

  const publicColumns = [
    {
      title: t('education/programmingcontest/contest:contest'),
      field: "contestName",
      render: (rowData) => (
        <Link
          to={{
            pathname:
              "/programming-contest/student-view-contest-detail/" +
              rowData["contestId"],
          }}
        >
          {rowData["contestName"]}
        </Link>
      ),
    },
    {
      title: t("common:status"),
      render: (rowData) => `${contestStatuses.find(item => item.value === rowData.status)?.label || ""}`
    },
    {title: t('common:manager'), field: "createdBy"},
    // {title: "Created At", field: "createdAt"},
  ];

  function getContestList() {
    request("get", "/students/contests", (res) => {
      const data = res.data.contests.map((e, index) => ({
        index: index + 1,
        contestId: e.contestId,
        contestName: e.contestName,
        status: e.statusId,
        createdBy: e.userId,
        createdAt: toFormattedDateTime(e.startAt),
      }));
      setContests(data);
    })
    // .then(() => setLoading(false));
  }

  function getPublicContestList() {
    request("get", "/contests/public", (res) => {
      const data = res.data.contests.map((e, index) => ({
        index: index + 1,
        contestId: e.contestId,
        contestName: e.contestName,
        status: e.statusId,
        createdBy: e.userId,
        createdAt: toFormattedDateTime(e.startAt),
      }));
      setPublicContests(data);
    });
  }

  useEffect(() => {
    getPublicContestList();
    getContestList();
  }, []);

  const handleChange = (event, newValue) => {
    setSelectedTab(newValue);
  };

  return (
    <Paper elevation={1} sx={{padding: "16px 24px", borderRadius: 4}}>
      {loading && <LinearProgress/>}
      <AntTabs
        value={selectedTab}
        onChange={handleChange}
        aria-label="contest tabs"
        scrollButtons="auto"
        variant="scrollable"
      >
        <AntTab label={t("common:public")} {...a11yProps(0)} />
        <AntTab label={t("common:registered")} {...a11yProps(1)} />
      </AntTabs>

      <TabPanel value={selectedTab} index={0} dir={"ltr"}>
          <StandardTable
            columns={publicColumns}
            data={publicContests}
            hideCommandBar
            options={{
              selection: false,
              pageSize: 5,
              search: true,
              sorting: true,
            }}
            components={{
              Container: (props) => <Paper {...props} elevation={0}/>,
            }}
          />
      </TabPanel>

      <TabPanel value={selectedTab} index={1} dir={"ltr"}>
          <StandardTable
            columns={registeredColumns}
            data={contests}
            hideCommandBar
            options={{
              selection: false,
              pageSize: 5,
              search: true,
              sorting: true,
            }}
            components={{
              Container: (props) => <Paper {...props} elevation={0}/>,
            }}
          />
      </TabPanel>
    </Paper>
  );
}
