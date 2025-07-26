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
import ExamClassCreate from "./ExamClassCreate";

function ExamClassList() {
  const [examClasses, setExamClasses] = useState([]);
  const [createDialogOpen, setCreateDialogOpen] = useState(false);
  const {t} = useTranslation("common");
  
  const columns = [
    {
      title: t("name"),
      field: "name",
      render: (rowData) => (
        <Link to={`/exam-class/detail/${rowData.id}`}>{rowData.name}</Link>
      ),
    },
    {title: t("description"), field: "description"},
  ];

  function getExamClassList() {
    let successHandler = (res) => setExamClasses(res.data);
    let errorHandlers = {
      onError: () => errorNoti(t("common:error"), 3000),
    };
    request("GET", "/exam-classes", successHandler, errorHandlers);
  }

  useEffect(getExamClassList, []);

  const handleCreateSuccess = () => {
    getExamClassList(); // Refresh the list after creating
  };

  return (
    <>
      <Paper elevation={1} sx={{padding: "16px 24px", borderRadius: 4}}>
        <Stack direction="row" justifyContent='space-between' mb={1.5}>
          <Typography variant="h6">{t("examClassList")}</Typography>
          <PrimaryButton
            startIcon={<AddIcon/>}
            onClick={() => setCreateDialogOpen(true)}>
            {t("create", {name: ''})}
          </PrimaryButton>
        </Stack>
        <StandardTable
          columns={columns}
          data={examClasses}
          hideCommandBar
          hideToolBar
          options={{
            pageSize: 10,
            selection: false,
            search: false,
            sorting: true,
          }}
          components={{
            Container: (props) => <Paper {...props} elevation={0}/>,
          }}
        />
      </Paper>
      
      <ExamClassCreate
        open={createDialogOpen}
        onClose={() => setCreateDialogOpen(false)}
        onSuccess={handleCreateSuccess}
      />
    </>
  );
}

const screenName = "SCR_EXAM_CLASS_LIST";
export default withScreenSecurity(ExamClassList, screenName, true);