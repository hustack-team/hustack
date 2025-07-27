import DoneIcon from "@mui/icons-material/Done";
import {Box, Chip, LinearProgress, Paper, Typography} from "@mui/material";
import React, {useEffect, useState} from "react";
import {useTranslation} from "react-i18next";
import {Link, useHistory, useParams} from "react-router-dom";
import {localeOption} from "utils/NumberFormat";
import {request} from "../../../api";
import StandardTable from "../../table/StandardTable";
import {getColorLevel} from "./lib";
import {getLevels} from "./CreateProblem";
import {errorNoti} from "../../../utils/notification";
import {MTableToolbar} from "material-table";

export default function StudentViewProblemList() {
  const {t} = useTranslation(["education/programmingcontest/studentviewcontestdetail", "education/programmingcontest/problem", "education/programmingcontest/testcase", "common"]);
  const levels = getLevels(t);
  const history = useHistory();

  const {contestId} = useParams();
  const [problems, setProblems] = useState([]);
  const [loading, setLoading] = useState(false);
  const [totalSubmittedPoints, setTotalSubmittedPoints] = useState(0);
  const [totalMaxPoints, setTotalMaxPoints] = useState(0);

  const columns = [
    {
      title: t("problem"),
      field: "problemName",
      render: (rowData) => (
        <Link
          to={`/programming-contest/student-view-contest-problem-detail/${contestId}/${rowData.problemId}`}
          style={{
            textDecoration: "none",
            color: "blue",
            cursor: "",
          }}
        >
          {rowData["problemName"]}
        </Link>
      ),
    },
    {
      title: t("problemCode"),
      field: "problemCode",
      align: "left",
      cellStyle: {minWidth: 120},
      render: (rowData) => (
        <Typography component="span" variant="body2" fontFamily="monospace">
          {rowData.problemCode}
        </Typography>
      )
    },
    {
      title: t("level"),
      field: "levelId",
      align: 'center',
      cellStyle: {minWidth: 120, paddingRight: 40},
      render: (rowData) => (
        <Typography component="span" variant="subtitle2" sx={{color: getColorLevel(`${rowData.levelId}`)}}>
          {`${levels.find(item => item.value === rowData.levelId)?.label || ""}`}
        </Typography>
      ),
    },
    {
      title: t("common:maxSubmittedPoint"),
      field: "maxSubmittedPoint",
      type: 'numeric',
      render: (rowData) => (
        <>
          {rowData.maxSubmittedPoint &&
            rowData.maxSubmittedPoint.toLocaleString("fr-FR", localeOption)}
        </>
      ),
      align: "right",
      minWidth: 160,
    },
    {
      title: t("common:maxPoint"),
      field: "maxPoint",
      type: "numeric",
      render: (rowData) => (
        <>
          {rowData.maxPoint != null
            ? rowData.maxPoint.toLocaleString("fr-FR", localeOption)
            : ""}
        </>
      ),
      align: "right",
      minWidth: 160,
    },
    {
      title: t("common:complete"),
      field: "accepted",
      cellStyle: {paddingRight: 40},
      render: (rowData) => rowData.accepted && <DoneIcon color="success"/>,
      align: "center",
      minWidth: 160,
    },
    {
      title: t("tags"),
      sorting: false,
      render: (rowData) => (
        <Box>
          {rowData?.tags?.length > 0 &&
            rowData.tags.map((tag) => (
              <Chip
                key={tag}
                size="small"
                label={tag}
                sx={{
                  marginRight: "6px",
                  marginBottom: "6px",
                  border: "1px solid lightgray",
                  fontStyle: "italic",
                }}
              />
            ))}
        </Box>
      ),
    },
  ];

  function getContestDetail() {
    setLoading(true);
    request(
      "get",
      "/contests/" + contestId + "/problems/v2",
      (res) => {
        const problemsData = res.data;
        setProblems(problemsData);

        const totalSubmitted = problemsData.reduce(
          (sum, problem) => sum + (problem.maxSubmittedPoint || 0),
          0
        );
        const totalMax = problemsData.reduce(
          (sum, problem) => sum + (problem.maxPoint || 0),
          0
        );
        setTotalSubmittedPoints(totalSubmitted);
        setTotalMaxPoints(totalMax);
        setLoading(false);
      },
      {
        onError: (e) => {
          if (e.response && e.response.status === 403) {
            history.push("/programming-contest/student-list-contest-registered");
          } else {
            errorNoti(t("common:error"), 3000);
          }
          setLoading(false);
        }
      }
    );
  }

  useEffect(() => {
    getContestDetail();
  }, []);

  const getPointsColor = () => {
    const submitted = totalSubmittedPoints || 0;
    const max = totalMaxPoints || 0;
    if (submitted === 0) return "#f44336";
    if (submitted < max) return "#0288d1";
    return "#4caf50";
  };

  return (
    <>
      {loading && <LinearProgress/>}
      <Box
        sx={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          padding: "2px 7px",
          marginBottom: "-8px",
          // backgroundColor: "#f5f5f5",
          // borderBottom: "1px solid rgb(224, 224, 224)",
        }}
      >
        {!loading && (
          <Typography
            variant="h6"
            sx={{
              color: getPointsColor(),
            }}
          >
            {t("common:maxSubmittedPoint")}:{" "}
            {totalSubmittedPoints?.toFixed(2) || 0} /{" "}
            {t("common:maxPoint")}:{" "}
            {totalMaxPoints?.toFixed(2) || 0}
          </Typography>
        )}
      </Box>
      <StandardTable
        columns={columns}
        data={problems}
        hideCommandBar
        options={{
          selection: false,
          pageSize: 5,
          search: true
        }}
        components={{
          Container: (props) => <Paper {...props} elevation={0}/>,
          Toolbar: (toolBarProps) => (
            toolBarProps.hideToolBar ? null : (
              <Box
                sx={{
                  display: "flex",
                  justifyContent: "space-between",
                  alignItems: "center",
                  padding: "8px 8px",
                }}
              >
                <Box sx={{flexGrow: 1, display: "flex", justifyContent: "flex-end"}}>
                  <MTableToolbar
                    {...toolBarProps}
                    classes={{
                      highlight: {backgroundColor: "transparent"},
                    }}
                    searchFieldStyle={{
                      height: 40,
                      ...toolBarProps.options?.searchFieldStyle,
                    }}
                  />
                </Box>
              </Box>
            )
          ),
        }}
      />
    </>
  );
}