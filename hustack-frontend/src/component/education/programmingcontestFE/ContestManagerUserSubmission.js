import {LoadingButton} from "@mui/lab";
import {Divider, Grid, IconButton, Paper, Stack, TextField, Tooltip, Typography} from "@mui/material";
import {request, saveFile} from "api";
import HustModal from "component/common/HustModal";
import StandardTable from "component/table/StandardTable";
import React, {useEffect, useState} from "react";
import {Link} from "react-router-dom";
import {errorNoti, infoNoti, successNoti} from "utils/notification";
import ManagerSubmitCodeOfParticipant from "./ManagerSubmitCodeOfParticipant";
import {RejudgeButton} from "./RejudgeButton";
import {getStatusColor} from "./lib";
import {useTranslation} from "react-i18next";
import {mapLanguageToDisplayName} from "./Constant";
import TertiaryButton from "../../button/TertiaryButton";
import AutorenewIcon from "@mui/icons-material/Autorenew";
import PrimaryButton from "../../button/PrimaryButton";
import SearchIcon from "@mui/icons-material/Search";
import {localeOption} from "../../../utils/NumberFormat";
import {RiCodeSSlashLine} from "react-icons/ri";

const filterInitValue = {
  userId: "",
  problemId: "",
  statuses: [],
  languages: [],
  fromDate: null,
  toDate: null,
}

export default function ContestManagerUserSubmission(props) {
  const contestId = props.contestId;
  const {t} = useTranslation(["common", 'education/programmingcontest/problem', "education/programmingcontest/testcase", "education/programmingcontest/contest"]);

  const [contestSubmissions, setContestSubmissions] = useState([]);
  const [
    isOpenManagerSubmitCodeOfParticipant,
    setIsOpenManagerSubmitCodeOfParticipant,
  ] = useState(false);

  const [filter, setFilter] = useState(filterInitValue);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(5);
  const [totalCount, setTotalCount] = useState(0);

  const [loadingState, setLoadingState] = useState({
    judging: false,
    rejudging: false,
    exporting: false,
  });

  const handleLoadingStateChange = (key, value) => {
    setLoadingState(prev => ({...prev, [key]: value}));
  };

  function handleCloseManagerSubmitParticipantCode() {
    setIsOpenManagerSubmitCodeOfParticipant(false);
    setPage(0)
  }

  function handleRejudgeAll() {
    handleLoadingStateChange('rejudging', true);

    request(
      "post",
      "/submissions/" + contestId + "/batch-evaluation",
      (res) => {
        handleLoadingStateChange('rejudging', false);
        successNoti("Submissions will be rejudged", 3000);
      },
      {
        onError: (e) => {
          handleLoadingStateChange('rejudging', false);
        },
        403: () => {
          infoNoti(
            "You don't have privilege to perform this action. Contact admin if needed",
            3000
          );
        },
      }
    );
  }

  function handleJudgeAll() {
    handleLoadingStateChange('judging', true);

    request(
      "post",
      "/submissions/" + contestId + "/batch-non-evaluated-evaluation",
      (res) => {
        handleLoadingStateChange('judging', false);
        successNoti("Submissions will be judged", 3000);
      },
      {
        onError: (e) => {
          handleLoadingStateChange('judging', false);
        },
        403: () => {
          infoNoti(
            "You don't have privilege to perform this action. Contact admin if needed",
            3000
          );
        },
      }
    );
  }

  function handleExportParticipantSubmission() {
    handleLoadingStateChange('exporting', true);

    request("get",
      "/contests/" + contestId + "/judged-submissions",
      (res) => {
        handleLoadingStateChange('exporting', false);
        saveFile(`${contestId}.pdf`, res.data)
      },
      {
        onError: e => {
          handleLoadingStateChange('exporting', false);
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

  const generateColumns = () => {
    const columns = [
      {
        title: "ID",
        field: "contestSubmissionId",
        render: (rowData) => (
          <Link
            to={
              "/programming-contest/manager-view-contest-problem-submission-detail/" +
              rowData.contestSubmissionId
            }
          >
            {rowData.contestSubmissionId.substring(0, 6)}
          </Link>
        ),
      },
      {
        title: t("common:id", {name: t('common:user')}),
        field: "userId",
        cellStyle: {
          minWidth: 140,
        },
        render: (rowData) => (
          <Tooltip title={rowData.fullname} placement="bottom-start" arrow>
            {rowData.userId}
          </Tooltip>
        ),
      },
      {
        title: t("common:id", {name: t('education/programmingcontest/problem:problem')}),
        field: "problemId",
        cellStyle: {
          minWidth: 100,
        },
        render: (rowData) => (
          <Tooltip title={rowData.problemName} placement="bottom-start" arrow>
            {rowData.problemId}
          </Tooltip>
        ),
      },
      {
        title: t("education/programmingcontest/testcase:pass"),
        field: "testCasePass",
        cellStyle: {
          minWidth: 80,
        }
      },
      {
        title: t("status"),
        field: "status",
        cellStyle: {
          minWidth: 120,
        },
        render: (rowData) => (
          <span style={{color: getStatusColor(`${rowData.status}`)}}>
            {`${rowData.status}`}
          </span>
        ),
      },
      // {title: "Message", field: "message"},
      {
        title: t("education/programmingcontest/testcase:point"),
        field: "point",
        type: 'numeric',
        render: (rowData) =>
          rowData.point?.toLocaleString("fr-FR", localeOption),
      },
      {
        title: t('common:language'),
        field: "sourceCodeLanguage",
        cellStyle: {
          minWidth: 100,
        },
        render: (rowData) => mapLanguageToDisplayName(rowData.sourceCodeLanguage)
      },
      {
        title: "IP",
        field: "createdByIp",
      },
      // {
      //   title: t("common:codeAuthorship"),
      //   field: "codeAuthorship",
      //   cellStyle: {
      //     minWidth: 110,
      //   },
      // },
      {
        title: t("common:createdTime"),
        field: "createAt",
        cellStyle: {minWidth: 130},
      },
      // {title: "Man. Status", field: "managementStatus"},
      // {title: "Violation", field: "violationForbiddenInstruction"},
      {
        title: t("common:action"),
        align: "center",
        cellStyle: {minWidth: 120},
        render: (rowData) => (
          <Stack spacing={1} direction="row" justifyContent='center'>
            <RejudgeButton submissionId={rowData.contestSubmissionId}/>
            {/*<Tooltip title={t('education/programmingcontest/contest:detectCodeAuthorship')}>*/}
            {/*  <IconButton*/}
            {/*    variant="contained"*/}
            {/*    color="primary"*/}
            {/*    onClick={() => {*/}
            {/*      detectCodeAuthorship(rowData.contestSubmissionId);*/}
            {/*    }}*/}
            {/*  >*/}
            {/*    <RiCodeSSlashLine/>*/}
            {/*  </IconButton>*/}
            {/*</Tooltip>*/}
          </Stack>
        ),
      },
    ];
    return columns;
  };

  const handleDateChange = (key, newValue) => {
    setFilter((prevFilter) => ({
      ...prevFilter,
      [key]: newValue,
    }));
  };

  const handleFilterChange = (key, event) => {
    setFilter((prevFilter) => ({
      ...prevFilter,
      [key]: event.target.value,
    }));
  };

  const handleChangePage = (newPage) => {
    setPage(newPage);
  };

  const handleChangePageSize = (newSize) => {
    setPage(0)
    setPageSize(newSize)
  }

  const resetFilter = () => {
    setFilter(filterInitValue)
  }

  const handleSearch = () => {
    setLoading(true);
    let url = `/teacher/contests/${contestId}/submissions?page=${page}&size=${pageSize}`;

    {
      ["userId", "contestId", "problemId"].forEach(field => {
        if (filter[field]) {
          url += `&${field}=${filter[field]}`;
        }
      })
    }

    {
      ["fromDate", "toDate"].forEach(field => {
        if (filter[field]) {
          url += `&${field}=${filter[field].toISOString()}`;
        }
      })
    }

    url += `&languages=${filter.languages}`;
    url += `&statuses=${filter.statuses}`;

    request("GET",
      url,
      (res) => {
        setLoading(false)

        const data = res.data
        if (data.numberOfElements === 0 && data.number > 0) {
          setPage(0)
        } else {
          setContestSubmissions(data.content);
          setTotalCount(data.totalElements)
        }
      },
      {
        onError: (error) => {
          setLoading(false)
          errorNoti(t("common:error", 3000))
        }
      },
    );
  }

  function handleSubmitCodeParticipant() {
    setIsOpenManagerSubmitCodeOfParticipant(true);
  }

  const detectCodeAuthorship = (submissionId) => {
    request(
      "GET",
      `/submissions/${submissionId}/code-authorship`,
      (res) => {
        handleSearch()
      },
      {
        onError: (e) => {
          errorNoti(t("common:error"), 3000)
        },
      },
    );
  }

  useEffect(() => {
    handleSearch();
  }, [page, pageSize]);

  return (
    <>
      <Typography variant="h6" sx={{marginBottom: "12px"}}>{t("search")}</Typography>
      <Grid container spacing={3}>
        <Grid item xs={3}>
          <TextField
            size='small'
            fullWidth
            label={t("common:id", {name: t('common:user')})}
            value={filter.userId}
            onChange={e => handleFilterChange('userId', e)}
          />
        </Grid>
        <Grid item xs={3}>
          <TextField
            size='small'
            fullWidth
            label={t("common:id", {name: t('education/programmingcontest/problem:problem')})}
            value={filter.problemId}
            onChange={e => handleFilterChange('problemId', e)}
          />
        </Grid>
        {/*<Grid item xs={3}>*/}
        {/*  <StyledSelect*/}
        {/*    fullWidth*/}
        {/*    key={t("status")}*/}
        {/*    label={t("status")}*/}
        {/*    options={statuses}*/}
        {/*    value={filter.statuses}*/}
        {/*    sx={{minWidth: 'unset', mr: 'unset'}}*/}
        {/*    SelectProps={selectProps(statuses)}*/}
        {/*    onChange={e => handleFilterChange('statuses', e)}*/}
        {/*  />*/}
        {/*</Grid>*/}
        {/*<Grid item xs={3}>*/}
        {/*  <StyledSelect*/}
        {/*    fullWidth*/}
        {/*    key={t("common:language")}*/}
        {/*    label={t("common:language")}*/}
        {/*    options={languages}*/}
        {/*    value={filter.languages}*/}
        {/*    sx={{minWidth: 'unset', mr: 'unset'}}*/}
        {/*    SelectProps={selectProps(languages)}*/}
        {/*    onChange={e => handleFilterChange('languages', e)}*/}
        {/*  />*/}
        {/*</Grid>*/}
        {/*<LocalizationProvider dateAdapter={AdapterMoment}>*/}
        {/*  <Grid item xs={3}>*/}
        {/*    <DateTimePicker*/}
        {/*      ampm={false}*/}
        {/*      label={`${t("common:createdTime")} - ${t("common:fromDate").toLowerCase()}`}*/}
        {/*      value={filter.fromDate}*/}
        {/*      onChange={(newValue) => handleDateChange("fromDate", newValue)}*/}
        {/*      renderInput={(params) => <TextField {...params} size="small" fullWidth/>}*/}
        {/*      inputFormat="YYYY-MM-DD HH:mm"*/}
        {/*    />*/}
        {/*  </Grid>*/}
        {/*  <Grid item xs={3}>*/}
        {/*    <DateTimePicker*/}
        {/*      ampm={false}*/}
        {/*      label={`${t("common:createdTime")} - ${t("common:toDate").toLowerCase()}`}*/}
        {/*      value={filter.toDate}*/}
        {/*      onChange={(newValue) => handleDateChange("toDate", newValue)}*/}
        {/*      renderInput={(params) => <TextField {...params} size="small" fullWidth/>}*/}
        {/*      inputFormat="YYYY-MM-DD HH:mm"*/}
        {/*    />*/}
        {/*  </Grid>*/}
        {/*</LocalizationProvider>*/}
      </Grid>
      <Stack direction="row" justifyContent='flex-end' spacing={2} sx={{mt: 3}}>
        <TertiaryButton
          onClick={resetFilter}
          variant="outlined"
          startIcon={<AutorenewIcon/>}
        >
          {t("common:reset")}
        </TertiaryButton>
        <PrimaryButton
          disabled={loading}
          onClick={handleSearch}
          startIcon={<SearchIcon/>}
        >
          {t("common:search")}
        </PrimaryButton>
      </Stack>

      <Divider sx={{mt: 2, mb: 2}}/>

      <Stack direction="row" justifyContent='space-between' mb={1.5}>
        <Typography variant="h6">{t("education/programmingcontest/contest:submissionList")}</Typography>

        <Stack direction="row" justifyContent='flex-end' spacing={2}>
          <Tooltip title="Submit code as a participant" arrow>
            <TertiaryButton
              variant="outlined"
              onClick={handleSubmitCodeParticipant}
            >
              Submit Participant Code
            </TertiaryButton>
          </Tooltip>
          <Tooltip
            title="Judge all submissions that are NOT EVALUATED"
            arrow
          >
            <LoadingButton
              sx={{textTransform: 'none'}}
              loading={loadingState.judging}
              loadingPosition="center"
              variant="outlined"
              onClick={handleJudgeAll}
            >
              Judge All
            </LoadingButton>
          </Tooltip>
          {props.screenAuthorization?.has(`SCR_CONTEST_MANAGER.BTN_REJUDGE.VIEW`)
            && <Tooltip
              title="Rejudge all submissions in this contest"
              arrow
            >
              <LoadingButton
                sx={{textTransform: 'none'}}
                loading={loadingState.rejudging}
                loadingPosition="center"
                variant="outlined"
                onClick={handleRejudgeAll}
              >
                Rejudge All
              </LoadingButton>
            </Tooltip>}
          <Tooltip title="Export all submissions in this contest" arrow>
            <LoadingButton
              sx={{textTransform: 'none'}}
              loading={loadingState.exporting}
              loadingPosition="center"
              variant="outlined"
              onClick={handleExportParticipantSubmission}
            >
              Export
            </LoadingButton>
          </Tooltip>
        </Stack>
      </Stack>
      <StandardTable
        hideCommandBar
        hideToolBar
        columns={generateColumns()}
        data={contestSubmissions}
        options={{
          pageSize: pageSize,
          selection: false,
          search: false,
          sorting: false,
        }}
        localization={{
          toolbar: {
            searchPlaceholder: "Search by UserID or ProblemID",
          },
        }}
        components={{
          Container: (props) => <Paper {...props} elevation={0}/>
        }}
        isLoading={loading}
        page={page}
        totalCount={totalCount}
        onChangePage={handleChangePage}
        onChangeRowsPerPage={handleChangePageSize}
      />

      {/* <ManagerSubmitCodeOfParticipantDialog
        open={isOpenManagerSubmitCodeOfParticipant}
        onClose={handleCloseManagerSubmitParticipantCode}
        contestId={contestId}
      /> */}
      <HustModal
        open={isOpenManagerSubmitCodeOfParticipant}
        textOk={"OK"}
        onClose={handleCloseManagerSubmitParticipantCode}
        title={"Submit code of participant"}
      >
        <ManagerSubmitCodeOfParticipant contestId={contestId}/>
      </HustModal>
    </>
  );
}
