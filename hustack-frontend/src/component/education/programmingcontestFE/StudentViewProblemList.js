import DoneIcon from "@mui/icons-material/Done";
import {Box, Chip, LinearProgress, Paper, Typography} from "@mui/material";
import React, {useEffect, useState} from "react";
import {useTranslation} from "react-i18next";
import {Link, useParams} from "react-router-dom";
import {localeOption} from "utils/NumberFormat";
import {request} from "../../../api";
import StandardTable from "../../table/StandardTableV2";
import {getColorLevel} from "./lib";
import {getLevels} from "./CreateProblem";
import {errorNoti} from "../../../utils/notification";

export default function StudentViewProblemList() {
  const {t} = useTranslation(["education/programmingcontest/studentviewcontestdetail", "education/programmingcontest/problem", "education/programmingcontest/testcase", "common"]);
  const levels = getLevels(t);

  const {contestId} = useParams();
  const [problems, setProblems] = useState([]);

  const [loading, setLoading] = useState(false);
  const [totalSubmittedPoints, setTotalSubmittedPoints] = useState(0);
  const [totalMaxPoints, setTotalMaxPoints] = useState(0);

  const columns = [
    // {
    //   title: "Code",
    //   field: "problemCode",
    // },
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
      title: t("education/programmingcontest/testcase:point"),
      field: "maxSubmittedPoint",
      type: 'numeric',
      render: (rowData) => (
        <>
          {
            rowData.maxSubmittedPoint &&
            rowData.maxSubmittedPoint.toLocaleString("fr-FR", localeOption)
            // (
            //   <Chip
            //     size="small"
            //     color="primary"
            //     variant="outlined"
            //     label={rowData.maxSubmittedPoint}
            //     sx={{
            //       padding: "4px",
            //       border: "2px solid lightgray",
            //       width: "52px",
            //     }}
            //   />
            // )
          }
        </>
      ),
      align: "right",
      minWidth: 160,
    },
    {
      title: t("maxPoint"),
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
          {rowData?.tags.length > 0 &&
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
      },
      {
        onError: (e) => {
          errorNoti(t("common:error"), 3000);
        }
      }
    ).finally(() => setLoading(false));
    // .then(() => setLoading(false));
  }

  useEffect(() => {
    getContestDetail();
  }, []);

  return (
    <>
      {loading && <LinearProgress/>}
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
        }}
        totalSubmittedPoints={totalSubmittedPoints}
        totalMaxPoints={totalMaxPoints}
      />
    </>
  );
}