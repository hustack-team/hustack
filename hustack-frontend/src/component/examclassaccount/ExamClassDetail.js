import {useParams} from "react-router";
import React, {useEffect, useRef, useState} from "react";
import {request, saveFile} from "../../api";
import {errorNoti, successNoti} from "../../utils/notification";
import {LoadingButton} from "@mui/lab";
import {Chip, Divider, Grid, Paper, Stack, Typography,} from "@mui/material";
import StandardTable from "../table/StandardTable";
import XLSX from "xlsx";
import TertiaryButton from "../button/TertiaryButton";
import CustomizedDialogs from "../dialog/CustomizedDialogs";
import {makeStyles} from "@material-ui/core/styles";
import withScreenSecurity from "../withScreenSecurity";
import {ConfirmDeleteDialog} from "../dialog/ConfirmDeleteDialog";
import {detail} from "../education/programmingcontestFE/ContestProblemSubmissionDetailViewedByManager";
import {useTranslation} from "react-i18next";
import ProgrammingContestLayout from "../education/programmingcontestFE/ProgrammingContestLayout";
import {useHistory} from "react-router-dom";
import {toFormattedDateTime} from "../../utils/dateutils";
import {InputFileUpload} from "../education/programmingcontestFE/StudentViewProgrammingContestProblemDetailV2";
import FileUploadIcon from '@mui/icons-material/FileUpload';

const useStyles = makeStyles((theme) => ({
  btn: {width: 100},
  dialogContent: {minWidth: 440, minHeight: 96},
  actions: {paddingRight: theme.spacing(2)},
}));

function ExamClassDetail() {
  const classes = useStyles();
  const {t} = useTranslation([
    "education/programmingcontest/problem",
    "common",
    "validation",
  ]);
  const history = useHistory();
  const params = useParams();
  const examClassId = params.id;
  const [name, setName] = useState();
  const [description, setDescription] = useState();
  const [executeDate, setExecuteDate] = useState();
  const [importedExcelFile, setImportedExcelFile] = useState(null);
  const [isProcessing, setIsProcessing] = useState(false);
  const [mapUserLogins, setMapUserLogins] = useState([]);
  const [loadingState, setLoadingState] = useState({
    exportingPDF: false,
  });
  const [openDialog, setOpen] = useState(false);
  const [resetPasswordResult, setResetPasswordResult] = useState();
  const [openModalConfirmDelete, setOpenModalConfirmDelete] = useState(false);
  const inputRef = useRef();

  const handleClose = () => {
    setOpen(false);
  };

  const handleCloseConfirmDeleteModal = () => {
    setOpenModalConfirmDelete(false)
  }

  const handleLoadingStateChange = (key, value) => {
    setLoadingState(prev => ({...prev, [key]: value}));
  };

  const columns = [
    {title: "Username / Email", field: "realUserLoginId"},
    {title: "Student code", field: "studentCode"},
    {title: "Full name", field: "fullname"},
    {title: "Keycloak username", field: "randomUserLoginId"},
    {title: "Password", field: "password"},
    {title: "Status", field: "status"},
  ];

  function importStudentsFromExcel(event) {
    event.preventDefault();

    setIsProcessing(true);
    const formData = new FormData();
    formData.append("file", importedExcelFile);

    let successHandler = (res) => {
      successNoti("Import sinh viên thành công", true);
      setIsProcessing(false);
      handleDeleteSelectedFile()
      getExamClassDetail()
    };
    let errorHandlers = {
      onError: (error) => {
        setIsProcessing(false);
        errorNoti("Đã xảy ra lỗi khi import sinh viên", true);
      },
    };

    const config = {
      headers: {
        "content-type": "multipart/form-data",
      },
    };

    request(
      "POST",
      `/exam-classes/${examClassId}/accounts/import`,
      successHandler,
      errorHandlers,
      formData,
      config
    );
  }

  function getExamClassDetail() {
    request("get", `/exam-classes/${examClassId}`, (res) => {
      //setLoading(false);
      setName(res.data.name);
      setDescription(res.data.description);
      setExecuteDate(res.data.executeDate);
      setMapUserLogins(res.data.accounts);
    });
  }

  const downloadHandler = (event) => {
    if (mapUserLogins.length === 0) {
      return;
    }

    var wbcols = [];

    wbcols.push({wpx: 80});
    wbcols.push({wpx: 120});
    let rows = mapUserLogins.length;
    for (let i = 0; i < rows; i++) {
      wbcols.push({wpx: 50});
    }
    wbcols.push({wpx: 50});

    let datas = [];

    for (let i = 0; i < mapUserLogins.length; i++) {
      let data = {};
      data["Original"] = mapUserLogins[i].realUserLoginId;
      data["Fullname"] = mapUserLogins[i].fullname;
      data["MSSV"] = mapUserLogins[i].studentCode;
      data["UserName"] = mapUserLogins[i].randomUserLoginId;
      data["Password"] = mapUserLogins[i].password;

      datas[i] = data;
    }

    var sheet = XLSX.utils.json_to_sheet(datas);
    var wb = XLSX.utils.book_new();
    sheet["!cols"] = wbcols;

    XLSX.utils.book_append_sheet(wb, sheet, "students");
    XLSX.writeFile(wb, `${name}.xlsx`);
  };

  function exportPdf() {
    handleLoadingStateChange('exportingPDF', true);

    request("GET",
      `/exam-classes/${examClassId}/accounts/export`,
      (res) => {
        handleLoadingStateChange('exportingPDF', false);
        saveFile(`${name}.pdf`, res.data)
      },
      {
        onError: e => {
          handleLoadingStateChange('exportingPDF', false);
          errorNoti(t("common:error", 3000))
        }
      },
      {},
      {
        responseType: "blob",
        headers: {
          "Accept": "application/pdf"
        }
      }
    );
  }

  function onFileChange(event) {
    setImportedExcelFile(event.target.files[0]);
  }

  const handleDeleteSelectedFile = () => {
    setImportedExcelFile(null)
    inputRef.current.value = null;
  }

  function deleteAccounts() {
    handleCloseConfirmDeleteModal()

    request(
      "DELETE",
      `/exam-classes/${examClassId}/accounts`,
      (res) => {
        const data = res.data

        data.title = "Xoá tài khoản"
        setResetPasswordResult(data)
        setOpen(true)
        getExamClassDetail();
      },
      {
        onError: (e) => {
          errorNoti(t("common:error", 3000))
        }
      }
    );
  }

  const generateAccounts = () => {
    request(
      "POST",
      `/exam-classes/${examClassId}/accounts`,
      (res) => {
        const data = res.data

        data.title = "Sinh tài khoản"
        setResetPasswordResult(data)
        setOpen(true)
        getExamClassDetail()
      },
      {
        onError: (e) => {
          errorNoti(t("common:error", 3000))
        },
      },
    );
  }

  const handleResetPassword = () => {
    request(
      "PATCH",
      `/exam-classes/${examClassId}/accounts/reset-password`,
      (res) => {
        const data = res.data

        data.title = "Sinh lại mật khẩu"
        setResetPasswordResult(data)
        setOpen(true)
        getExamClassDetail()
      },
      {
        onError: (e) => {
          errorNoti(t("common:error", 3000))
        },
      },
    );
  }

  const updateStatus = (enabled) => {
    request(
      "PATCH",
      `/exam-classes/${examClassId}/accounts`,
      (res) => {
        const data = res.data

        if (enabled) {
          data.title = "Kích hoạt tài khoản"
        } else {
          data.title = "Vô hiệu hoá tài khoản"
        }

        setResetPasswordResult(data)
        setOpen(true)
        getExamClassDetail()
      },
      {
        onError: (e) => {
          errorNoti(t("common:error", 3000))
        },
      },
      {enabled: enabled}
    );
  }

  const handleExit = () => {
    history.push(`/exam-class/list`);
  }

  useEffect(() => {
    getExamClassDetail();
  }, []);

  return (
    <ProgrammingContestLayout title={t("")} onBack={handleExit}>
      <Stack direction="row" spacing={2} mb={1.5} justifyContent="space-between">
        <Typography variant="h6" component='span'>
          {t("generalInfo")}
        </Typography>
      </Stack>

      <Grid container spacing={2}>
        {[
          [t("Name"), name],
          [t("common:description"), description],
          [t("Date"), toFormattedDateTime(executeDate)],
        ].map(([key, value, sx, helpText]) => (
          <Grid item xs={12} sm={12} md={3}>
            {detail(key, value, sx, helpText)}
          </Grid>
        ))}
      </Grid>

      <Stack direction="row" spacing={1} mt={2}>
        <InputFileUpload
          id="exam-class-selected-upload-file"
          label={t("common:selectFile")}
          onChange={onFileChange}
          ref={inputRef}
        />
        {importedExcelFile && (
          <Chip
            color="success"
            variant="outlined"
            label={importedExcelFile.name}
            onDelete={handleDeleteSelectedFile}
          />
        )}
        <LoadingButton
          loading={isProcessing}
          startIcon={<FileUploadIcon/>}
          disabled={!importedExcelFile}
          color="primary"
          variant="contained"
          onClick={importStudentsFromExcel}
          sx={{textTransform: 'none'}}
        >
          Tải lên
        </LoadingButton>
      </Stack>
      <Stack direction={"row"} spacing={2} mt={2}>
        <LoadingButton
          sx={{textTransform: 'none'}}
          loading={loadingState.exportingPDF}
          loadingPosition="center"
          variant="outlined"
          disabled={!(mapUserLogins?.length > 0)}
          onClick={exportPdf}
        >
          Xuất PDF
        </LoadingButton>
        <TertiaryButton variant="outlined" disabled={!(mapUserLogins?.length > 0)} onClick={downloadHandler}>
          Xuất Excel
        </TertiaryButton>
        <TertiaryButton variant="outlined" disabled={!(mapUserLogins?.length > 0)} onClick={generateAccounts}>
          Sinh tài khoản
        </TertiaryButton>
        <TertiaryButton variant="outlined" disabled={!(mapUserLogins?.length > 0)} onClick={handleResetPassword}>
          Sinh lại mật khẩu
        </TertiaryButton>
        <TertiaryButton onClick={() => updateStatus(true)}
                        variant="outlined" disabled={!(mapUserLogins?.length > 0)}>
          Kích hoạt tài khoản
        </TertiaryButton>
        <TertiaryButton onClick={() => updateStatus(false)}
                        variant="outlined" disabled={!(mapUserLogins?.length > 0)}>
          Vô hiệu hoá tài khoản
        </TertiaryButton>
        <TertiaryButton variant="outlined" color="error" disabled={!(mapUserLogins?.length > 0)}
                        onClick={() => setOpenModalConfirmDelete(true)}>
          Xoá tài khoản
        </TertiaryButton>
      </Stack>

      <Divider sx={{mt: 2, mb: 2}}/>

      <Stack direction="row" justifyContent='space-between' mb={1.5}>
        <Typography variant="h6">Accounts</Typography>
      </Stack>
      <StandardTable
        columns={columns}
        data={mapUserLogins}
        hideCommandBar
        options={{
          selection: false,
          pageSize: 10,
          search: true,
          sorting: true,
        }}
        components={{
          Container: (props) => <Paper {...props} elevation={0}/>,
        }}
      />

      <CustomizedDialogs
        open={openDialog}
        title={resetPasswordResult?.title}
        handleClose={handleClose}
        contentTopDivider
        content={
          typeof (resetPasswordResult?.success) === 'number' && !isNaN(resetPasswordResult.success) ?
            <>
              <Typography>
                Thành công: {resetPasswordResult?.success}
              </Typography>
              <Typography>
                Thất bại: {resetPasswordResult?.fail}
              </Typography>
            </>
            : <Typography>
              Không tìm thấy tài khoản nào thoả mãn điều kiện
            </Typography>
        }
        classNames={{content: classes.dialogContent, actions: classes.actions}}
      />

      <ConfirmDeleteDialog open={openModalConfirmDelete}
                           handleClose={handleCloseConfirmDeleteModal}
                           handleDelete={deleteAccounts}
                           entity='tất cả tài khoản'
      />
    </ProgrammingContestLayout>
  );
}

const screenName = "SCR_EXAM_CLASS_DETAIL";
export default withScreenSecurity(ExamClassDetail, screenName, true);
