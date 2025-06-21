import React, {useEffect, useState} from "react";
import {Link} from "react-router-dom";
import {Paper, Stack, Typography} from "@mui/material";
import AddIcon from "@mui/icons-material/Add";
import StandardTable from "../table/StandardTable";
import {errorNoti} from "../../utils/notification";
import {request} from "../../api";
import withScreenSecurity from "../withScreenSecurity";
import PrimaryButton from "../button/PrimaryButton";
import {useTranslation} from "react-i18next";

function ExamClassList() {
  const [examClasses, setExamClasses] = useState([]);
  const {t} = useTranslation(["education/programmingcontest/problem", "common"]);
  const columns = [
    {
      title: "Name",
      field: "name",
      render: (rowData) => (
        <Link to={`/exam-class/detail/${rowData.id}`}>{rowData.name}</Link>
      ),
    },
    {title: "Date", field: "execute_date"},
  ];

  function getExamClassList() {
    let successHandler = (res) => setExamClasses(res.data);
    let errorHandlers = {
      onError: () => errorNoti("Đã xảy ra lỗi khi tải dữ liệu", true),
    };
    request("GET", "/exam-classes", successHandler, errorHandlers);
  }

  useEffect(getExamClassList, []);

  return (
    <Paper elevation={1} sx={{padding: "16px 24px", borderRadius: 4}}>
      <Stack direction="row" justifyContent='space-between' mb={1.5}>
        <Typography variant="h6">Exam Class List</Typography>
        <PrimaryButton
          startIcon={<AddIcon/>}
          onClick={() => {
            window.open("/exam-class/create");
          }}>
          {t("common:create", {name: ''})}
        </PrimaryButton>
      </Stack>
      <StandardTable
        columns={columns}
        data={examClasses}
        hideCommandBar
        options={{
          selection: false,
          search: true,
          sorting: true,
        }}
        components={{
          Container: (props) => <Paper {...props} elevation={0}/>,
        }}
      />
    </Paper>
  );
}

const screenName = "SCR_EXAM_CLASS_LIST";
export default withScreenSecurity(ExamClassList, screenName, true);