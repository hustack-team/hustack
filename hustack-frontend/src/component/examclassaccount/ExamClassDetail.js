import {useParams} from "react-router";
import React, {useEffect, useRef, useState} from "react";
import {request, saveFile} from "../../api";
import {errorNoti, infoNoti, successNoti} from "../../utils/notification";
import {LoadingButton} from "@mui/lab";
import {Button, Chip, Divider, Grid, IconButton, Paper, Stack, Tooltip, Typography,} from "@mui/material";
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
import {InputFileUpload} from "../education/programmingcontestFE/StudentViewProgrammingContestProblemDetailV2";
import FileUploadIcon from '@mui/icons-material/FileUpload';
import Box from "@mui/material/Box";
import DeleteIcon from "@mui/icons-material/Delete";
import PersonAddAltRoundedIcon from '@mui/icons-material/PersonAddAltRounded';
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline';
import BlockIcon from '@mui/icons-material/Block';
import LockResetIcon from '@mui/icons-material/LockReset';
import _ from "lodash";

const useStyles = makeStyles((theme) => ({
  btn: {width: 100},
  dialogPaper: {maxWidth: 960},
  dialogContent: {minWidth: 440, minHeight: 96},
  dialogContentWithList: {minWidth: 960},
  resetPasswordConfirmDialogContent: {minWidth: 440},
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
    exportingExcel: false,
    generateAccounts: false,
    resetPassword: false,
    enableAccounts: false,
    disableAccounts: false,
    deletingAccounts: false,
  });
  const [openDialog, setOpenDialog] = useState({
    resetPassword: false,
    actionResult: false,
    confirmDelete: false,
    confirmDeleteSingle: false,
  });
  const [actionResult, setActionResult] = useState();
  const [selectedUser, setSelectedUser] = useState(null);
  const inputRef = useRef();

  const handleCloseResetPasswordConfirmDialog = () => {
    handleOpenDialogChange('resetPassword', false)
  }

  const handleCloseActionResultDialog = () => {
    handleOpenDialogChange('actionResult', false);
  };

  const handleCloseConfirmDeleteModal = () => {
    handleOpenDialogChange('confirmDelete', false);
  }

  const handleCloseConfirmDeleteSingleModal = () => {
    handleOpenDialogChange('confirmDeleteSingle', false);
    setSelectedUser(null);
  }

  const handleLoadingStateChange = (key, value) => {
    setLoadingState(prev => ({...prev, [key]: value}));
  };

  const handleOpenDialogChange = (key, value) => {
    setOpenDialog(prev => ({...prev, [key]: value}));
  };

  const handleGenerateSingleAccount = (user) => {
    request(
      "POST",
      `/exam-classes/${examClassId}/accounts/${user.id}`,
      (res) => {
        const code = res?.data?.code;
        if (code === 1006) {
          successNoti(t("common:generateAccountSuccess"), 3000);
        } else if (code === 10003) {
          infoNoti(t("common:accountAlreadyGenerated"));
        } else if (code === 10004) {
          errorNoti(t("common:accountGenerationFailed"), 3000);
        }
        getExamClassDetail();
      },
      {
        404: handleAccountNotFound(t),
        onError: (e) => errorNoti(t("common:error"), 3000),
      }
    );
  };

  function handleAccountNotFound(t) {
    return (e) => {
      if (e?.response?.data?.code === 10002) {
        errorNoti(t("common:accountNotFoundInExamClass"), 3000);
      } else {
        errorNoti(t("common:error"), 3000);
      }
    };
  }

  const handleRegenerateSinglePassword = (user) => {
    request(
      "PATCH",
      `/exam-classes/${examClassId}/accounts/${user.id}/reset-password`,
      (res) => {
        const code = res?.data?.code;
        if (code === 1003) {
          successNoti(t("common:regeneratePasswordSuccess"), 3000);
        } else if (code === 10001) {
          errorNoti(t("common:accountNotGenerated"), 3000);
        }
        getExamClassDetail();
      },
      {
        404: handleAccountNotFound(t),
        onError: (e) => errorNoti(t("common:error"), 3000),
      }
    );
  };

  const handleUpdateSingleAccountStatus = (user, enabled) => {
    request(
      "PATCH",
      `/exam-classes/${examClassId}/accounts/${user.id}/status`,
      (res) => {
        const code = res?.data?.code;
        if (code === 1004) {
          successNoti(enabled ? t("common:enableAccountSuccess") : t("common:disableAccountSuccess"), 3000);
        } else if (code === 10001) {
          errorNoti(t("common:accountNotGenerated"), 3000);
        }
        getExamClassDetail();
      },
      {
        404: handleAccountNotFound(t),
        onError: (e) => errorNoti(t("common:error"), 3000),
      },
      {enabled: enabled}
    );
  };

  const handleDeleteSingleAccount = () => {
    handleCloseConfirmDeleteSingleModal();

    if (!selectedUser) return;
    request(
      "DELETE",
      `/exam-classes/${examClassId}/accounts/${selectedUser.id}`,
      (res) => {
        const code = res?.data?.code;
        if (code === 1001) {
          successNoti(t("common:deleteSuccess", {name: t("common:account")}), 3000);
        } else if (code === 1002) {
          infoNoti(t("common:accountDisabled"), 3000);
        }
        getExamClassDetail();
      },
      {
        404: handleAccountNotFound(t),
        onError: (e) => errorNoti(t("common:error"), 3000),
      }
    );
  };

  const columns = [
    {
      title: t("common:usernameOrEmail"),
      field: "realUserLoginId"
    },
    {
      title: t("common:studentCode"),
      field: "studentCode",
      cellStyle: {
        minWidth: 160
      }
    },
    {
      title: t("common:fullName"),
      field: "fullname",
      cellStyle: {
        minWidth: 120
      }
    },
    {
      title: t("common:username"),
      field: "randomUserLoginId",
      cellStyle: {
        minWidth: 170
      }
    },
    {
      title: t("common:password"),
      field: "password"
    },
    {
      title: t("common:status"),
      field: "status", cellStyle: {
        minWidth: 120
      }

    },
    {
      title: t("common:action"),
      sorting: false,
      align: "center",
      render: (rowData) => (
        <Stack spacing={1} direction="row" justifyContent='flex-end'>
          {_.isEmpty(rowData.randomUserLoginId?.trim()) &&
            <Tooltip title={t('common:generateAccount')}>
              <IconButton
                color="primary"
                onClick={() => handleGenerateSingleAccount(rowData)}
              >
                <PersonAddAltRoundedIcon/>
              </IconButton>
            </Tooltip>
          }

          {!_.isEmpty(rowData.randomUserLoginId?.trim()) &&
            <Tooltip title={t('common:regeneratePassword')}>
              <IconButton
                variant="contained"
                color="primary"
                onClick={() => handleRegenerateSinglePassword(rowData)}
              >
                <LockResetIcon/>
              </IconButton>
            </Tooltip>
          }

          {!_.isEmpty(rowData.randomUserLoginId?.trim()) && rowData.status === 'DISABLED' &&
            <Tooltip title={t('common:enableAccount')}>
              <IconButton
                variant="contained"
                color="success"
                onClick={() => handleUpdateSingleAccountStatus(rowData, true)}>
                <CheckCircleOutlineIcon/>
              </IconButton>
            </Tooltip>
          }

          {!_.isEmpty(rowData.randomUserLoginId?.trim()) && rowData.status === 'ACTIVE' &&
            <Tooltip title={t('common:disableAccount')}>
              <IconButton
                variant="contained"
                color="error"
                onClick={() => handleUpdateSingleAccountStatus(rowData, false)}>
                <BlockIcon/>
              </IconButton>
            </Tooltip>
          }

          <Tooltip title={t('common:deleteAccount')}>
            <IconButton
              variant="contained"
              color="error"
              onClick={() => {
                setSelectedUser(rowData);
                handleOpenDialogChange('confirmDeleteSingle', true);
              }}
            >
              <DeleteIcon/>
            </IconButton>
          </Tooltip>
        </Stack>),
    }
  ];

  // Columns for result table with reason column
  const resultColumns = [
    ...columns.slice(0, -1), // Take all columns except the last action column
    {
      title: t("common:reason"),
      field: "reason",
      cellStyle: {
        minWidth: 200
      }
    }
  ];

  function importStudentsFromExcel(event) {
    event.preventDefault();

    setIsProcessing(true);
    const formData = new FormData();
    formData.append("file", importedExcelFile);

    let successHandler = (res) => {
      successNoti(t('common:importSuccess'), 3000);
      setIsProcessing(false);
      handleDeleteSelectedFile()
      getExamClassDetail()
    };
    let errorHandlers = {
      onError: (error) => {
        setIsProcessing(false);
        errorNoti(t("common:error"), 3000)
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
      const examClassData = res.data.data;
      setName(examClassData.name);
      setDescription(examClassData.description);
      setExecuteDate(examClassData.executeDate);
      setMapUserLogins(examClassData.accounts);
    }, {
      404: () => {
        errorNoti(t("common:examClassNotFound"), 3000);
        handleExit();
      },
      onError: (e) => {
        errorNoti(t("common:error"), 3000);
      }
    });
  }

  const downloadHandler = (accounts) => {
    handleLoadingStateChange('exportingExcel', true);
    if (accounts.length === 0) {
      return;
    }

    // Check if any account has reason field
    const hasReason = accounts.some(account => account.reason);

    const wbcols = [];

    wbcols.push({wpx: 80});  // usernameOrEmail
    wbcols.push({wpx: 120}); // fullName
    wbcols.push({wpx: 100}); // studentCode
    wbcols.push({wpx: 120}); // username
    wbcols.push({wpx: 100}); // password
    if (hasReason) {
      wbcols.push({wpx: 150}); // reason
    }

    const datas = [];

    for (let i = 0; i < accounts.length; i++) {
      const data = {};
      data[t("common:usernameOrEmail")] = accounts[i].realUserLoginId;
      data[t("common:fullName")] = accounts[i].fullname;
      data[t("common:studentCode")] = accounts[i].studentCode;
      data[t("common:username")] = accounts[i].randomUserLoginId;
      data[t("common:password")] = accounts[i].password;
      if (hasReason) {
        data[t("common:reason")] = accounts[i].reason || "";
      }

      datas[i] = data;
    }

    var sheet = XLSX.utils.json_to_sheet(datas);
    var wb = XLSX.utils.book_new();
    sheet["!cols"] = wbcols;

    XLSX.utils.book_append_sheet(wb, sheet, "students");
    handleLoadingStateChange('exportingExcel', false);
    XLSX.writeFile(wb, `${name}.xlsx`);
  };

  function exportPdf() {
    handleLoadingStateChange('exportingPDF', true);

    request("GET",
      `/exam-classes/${examClassId}/accounts/export`,
      (res) => {
        saveFile(`${name}.pdf`, res.data)
      },
      {
        404: () => {
          errorNoti(t("common:examClassNotFound"), 3000);
        },
        onError: e => {
          errorNoti(t("common:error"), 3000)
        }
      },
      {},
      {
        responseType: "blob",
        headers: {
          "Accept": "application/pdf"
        }
      }
    ).finally(() => {
      handleLoadingStateChange('exportingPDF', false);
    });
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
    handleLoadingStateChange('deletingAccounts', true);

    request(
      "DELETE",
      `/exam-classes/${examClassId}/accounts`,
      (res) => {
        const data = res.data

        // Process response with disabledAccounts and failedAccounts
        if (data.disabled > 0 || data.failed > 0) {
          // Some accounts are disabled or failed

          // Add reason field for disabledAccounts
          const disabledAccountsWithReason = (data.disabledAccounts || []).map(account => ({
            ...account,
            reason: t("common:accountDisabled")
          }));

          // Add reason field for failedAccounts (empty)
          const failedAccountsWithReason = (data.failedAccounts || []).map(account => ({
            ...account,
            reason: ""
          }));

          const allAffectedAccounts = [
            ...disabledAccountsWithReason,
            ...failedAccountsWithReason
          ];

          data.success = data.deleted || 0;
          data.fail = data.disabled + data.failed;
          data.updateFailures = allAffectedAccounts;
        } else {
          // All accounts deleted successfully
          data.success = data.deleted || 0;
          data.fail = 0;
          data.updateFailures = [];
        }

        data.title = t('common:deleteAccount')
        setActionResult(data)
        handleOpenDialogChange('actionResult', true)
        getExamClassDetail();
      },
      {
        404: () => {
          errorNoti(t("common:examClassNotFound"), 3000);
        },
        onError: (e) => {
          errorNoti(t("common:error"), 3000)
        }
      }
    ).finally(() => {
      handleLoadingStateChange('deletingAccounts', false);
    });
  }

  const generateAccounts = () => {
    handleLoadingStateChange('generateAccounts', true);

    request(
      "POST",
      `/exam-classes/${examClassId}/accounts`,
      (res) => {
        const data = res.data

        data.title = t('common:generateAccount')
        setActionResult(data)
        handleOpenDialogChange('actionResult', true)
        getExamClassDetail()
      },
      {
        404: () => {
          errorNoti(t("common:examClassNotFound"), 3000);
        },
        onError: (e) => {
          errorNoti(t("common:error"), 3000)
        },
      },
    ).finally(() => {
      handleLoadingStateChange('generateAccounts', false);
    });
  }

  const handleResetPassword = () => {
    handleCloseResetPasswordConfirmDialog()
    handleLoadingStateChange('resetPassword', true);

    request(
      "PATCH",
      `/exam-classes/${examClassId}/accounts/reset-password`,
      (res) => {
        const data = res.data

        data.title = t('common:regeneratePassword')
        setActionResult(data)
        handleOpenDialogChange('actionResult', true)
        getExamClassDetail()
      },
      {
        404: () => {
          errorNoti(t("common:examClassNotFound"), 3000);
        },
        onError: (e) => {
          errorNoti(t("common:error"), 3000)
        },
      },
    ).finally(() => {
      handleLoadingStateChange('resetPassword', false);
    });
  }

  const updateStatus = (enabled) => {
    if (enabled) {
      handleLoadingStateChange('enableAccounts', true);
    } else {
      handleLoadingStateChange('disableAccounts', true);
    }

    request(
      "PATCH",
      `/exam-classes/${examClassId}/accounts`,
      (res) => {
        const data = res.data

        if (enabled) {
          data.title = t('common:enableAccount')
        } else {
          data.title = t('common:disableAccount')
        }

        setActionResult(data)
        handleOpenDialogChange('actionResult', true)
        getExamClassDetail()
      },
      {
        404: () => {
          errorNoti(t("common:examClassNotFound"), 3000);
        },
        onError: (e) => {
          errorNoti(t("common:error"), 3000)
        },
      },
      {enabled: enabled}
    ).finally(() => {
      if (enabled) {
        handleLoadingStateChange('enableAccounts', false);
      } else {
        handleLoadingStateChange('disableAccounts', false);
      }
    });
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
          [t("common:name"), name],
          [t("common:description"), description],
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
          {t('common:upload')}
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
          {t('common:exportPdf')}
        </LoadingButton>
        <LoadingButton
          sx={{textTransform: 'none'}}
          loading={loadingState.exportingExcel}
          loadingPosition="center"
          variant="outlined"
          disabled={!(mapUserLogins?.length > 0)}
          onClick={() => downloadHandler(mapUserLogins)}
        >
          {t('common:exportExcel')}
        </LoadingButton>
        <LoadingButton
          sx={{textTransform: 'none'}}
          loading={loadingState.generateAccounts}
          loadingPosition="center"
          variant="outlined"
          disabled={!(mapUserLogins?.length > 0)}
          onClick={generateAccounts}
        >
          {t('common:generateAccount')}
        </LoadingButton>
        <LoadingButton
          sx={{textTransform: 'none'}}
          loading={loadingState.resetPassword}
          loadingPosition="center"
          variant="outlined"
          disabled={!(mapUserLogins?.length > 0)}
          onClick={() => handleOpenDialogChange('resetPassword', true)}
        >
          {t('common:regeneratePassword')}
        </LoadingButton>
        <LoadingButton
          sx={{textTransform: 'none'}}
          loading={loadingState.enableAccounts}
          loadingPosition="center"
          variant="outlined"
          disabled={!(mapUserLogins?.length > 0)}
          onClick={() => updateStatus(true)}
        >
          {t('common:enableAccount')}
        </LoadingButton>
        <LoadingButton
          sx={{textTransform: 'none'}}
          loading={loadingState.disableAccounts}
          loadingPosition="center"
          variant="outlined"
          disabled={!(mapUserLogins?.length > 0)}
          onClick={() => updateStatus(false)}
        >
          {t('common:disableAccount')}
        </LoadingButton>
        <LoadingButton
          sx={{textTransform: 'none'}}
          loading={loadingState.deletingAccounts}
          loadingPosition="center"
          variant="outlined"
          color="error"
          disabled={!(mapUserLogins?.length > 0)}
          onClick={() => handleOpenDialogChange('confirmDelete', true)}
        >
          {t('common:deleteAccount')}
        </LoadingButton>
      </Stack>

      <Divider sx={{mt: 2, mb: 2}}/>

      <Stack direction="row" justifyContent='space-between' mb={1.5}>
        <Typography variant="h6">{t('common:account')}</Typography>
      </Stack>
      <StandardTable
        columns={columns}
        data={mapUserLogins}
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

      <CustomizedDialogs
        open={openDialog.resetPassword}
        handleClose={handleCloseResetPasswordConfirmDialog}
        title={t('common:regeneratePassword')}
        contentTopDivider
        content={
          <Typography variant="subtitle2">
            {t('common:regeneratePasswordConfirm')}
          </Typography>
        }
        actions={
          <>
            <TertiaryButton
              color="inherit"
              onClick={handleCloseResetPasswordConfirmDialog}>
              {t('common:cancel')}
            </TertiaryButton>

            <Button
              variant='contained'
              sx={{textTransform: 'none'}}
              onClick={handleResetPassword}>
              {t('common:regeneratePassword')}
            </Button>
          </>
        }
        classNames={{content: classes.resetPasswordConfirmDialogContent}}
      />

      <CustomizedDialogs
        open={openDialog.actionResult}
        title={actionResult?.title}
        handleClose={handleCloseActionResultDialog}
        contentTopDivider
        content={
          typeof (actionResult?.success) === 'number' && !isNaN(actionResult.success) ?
            <>
              <Typography variant="subtitle2">
                {t('common:success')}: {actionResult?.success}. {t('common:fail')}: {actionResult?.fail}
              </Typography>

              {actionResult?.fail > 0 &&
                <Box sx={{mt: 2, mb: 1, maxWidth: 960 - 2 * 16}}>
                  <Stack direction="row" spacing={2} mb={1.5} justifyContent="space-between" alignItems='center'>
                    <Typography variant="h6" component='span'>
                      <Typography variant="h6">{t('common:affectedAccountList')}</Typography>
                    </Typography>

                    <Stack direction="row" spacing={2}>
                      <LoadingButton
                        sx={{textTransform: 'none'}}
                        loading={loadingState.exportingExcel}
                        loadingPosition="center"
                        variant="contained"
                        disabled={!(actionResult?.updateFailures?.length > 0)}
                        onClick={() => downloadHandler(actionResult?.updateFailures || [])}
                      >
                        {t('common:download')}
                      </LoadingButton>
                    </Stack>
                  </Stack>
                  <StandardTable
                    columns={resultColumns}
                    data={actionResult?.updateFailures || []}
                    hideCommandBar
                    hideToolBar
                    options={{
                      selection: false,
                      pageSize: 5,
                      search: false,
                      sorting: true,
                    }}
                    components={{
                      Container: (props) => <Paper {...props} elevation={0}/>,
                    }}
                  />
                </Box>
              }
            </>
            : <Typography variant="subtitle2">
              {t('common:noAccountMatched')}
            </Typography>
        }
        classNames={{
          content: actionResult?.fail > 0 ? classes.dialogContentWithList : classes.dialogContent,
          paper: classes.dialogPaper,
          actions: classes.actions
        }}
      />

      <ConfirmDeleteDialog open={openDialog.confirmDelete}
                           handleClose={handleCloseConfirmDeleteModal}
                           handleDelete={deleteAccounts}
                           entity={t('common:allAccounts')}
      />

      <ConfirmDeleteDialog
        open={openDialog.confirmDeleteSingle}
        handleClose={handleCloseConfirmDeleteSingleModal}
        handleDelete={handleDeleteSingleAccount}
        entity={t('common:account')}
        name={selectedUser?.fullname}
      />
    </ProgrammingContestLayout>
  );
}

const screenName = "SCR_EXAM_CLASS_DETAIL";
export default withScreenSecurity(ExamClassDetail, screenName, true);
