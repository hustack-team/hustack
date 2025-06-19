/* eslint-disable */
import React, {useEffect, useState} from "react";
import {useHistory} from "react-router-dom";
import {request} from "../../../api";
import {Paper, Stack, Typography} from "@mui/material";
import StandardTable from "../../table/StandardTable";
import {errorNoti} from "../../../utils/notification";
import {useTranslation} from "react-i18next";

function StudentMyQuizTestList() {
  const {t} = useTranslation(["common"]);

  const history = useHistory();
  const [ListQuiz, setListQuizs] = useState([]);
  const [loading, setLoading] = useState(true);

  const onClickQuizId = (quizid, viewTypeId) => {
    //history.push("/edu/class/student/quiztest/detail", {
    //  testId: quizid,
    //  viewTypeId: viewTypeId,
    //});

    history.push("/edu/class/student/quiztest/detail/" + quizid);
  };
  const columns = [
    {
      title: "Mã Quiz Test",
      field: "testId",
      render: (rowData) =>
        rowData["statusId"] === "STATUS_APPROVED" ? (
          <a
            style={{cursor: "pointer"}}
            onClick={() => {
              onClickQuizId(rowData["testId"], rowData["viewTypeId"]);
            }}
          >
            {rowData["testId"]}
          </a>
        ) : (
          <p>{rowData["testId"]}</p>
        ),
    },
    {title: "Tên Quiz Test", field: "testName"},
  ];

  async function getQuizList() {
    setLoading(true);
    request(
      "get",
      "/get-my-quiz-test-list",
      (res) => {
        setListQuizs(res.data);
        setLoading(false);
      },
      {
        onError: e => {
          setLoading(false);
          errorNoti(t("common:error", 3000))
        }
      }
    );
  }

  useEffect(() => {
    getQuizList();
  }, []);

  return (
    <Paper elevation={1} sx={{padding: "16px 24px", borderRadius: 4}}>
      <Stack direction="row" justifyContent='space-between' mb={1.5}>
        <Typography variant="h6">Quiz Tests</Typography>
      </Stack>
      <StandardTable columns={columns}
                     data={ListQuiz}
                     hideCommandBar
                     options={{
                       selection: false,
                       pageSize: 5,
                       sorting: false,
                     }}
                     components={{
                       Container: (props) => <Paper {...props} elevation={0}/>,
                     }}
      />
    </Paper>
  );
}

export default StudentMyQuizTestList;
