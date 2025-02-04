import {Divider, Grid, Paper, Stack, TextField, Tooltip, Typography} from "@mui/material";
import {request} from "api";
import {RejudgeButton} from "component/education/programmingcontestFE/RejudgeButton";
import {getStatusColor} from "component/education/programmingcontestFE/lib";
import StandardTable from "component/table/StandardTable";
import withScreenSecurity from "component/withScreenSecurity";
import {Link} from "react-router-dom";
import StyledSelect from "../select/StyledSelect";
import TertiaryButton from "../button/TertiaryButton";
import AutorenewIcon from "@mui/icons-material/Autorenew";
import PrimaryButton from "../button/PrimaryButton";
import SearchIcon from "@mui/icons-material/Search";
import React, {useEffect, useState} from "react";
import {useTranslation} from "react-i18next";
import {selectProps} from "../education/programmingcontestFE/ListProblemContent";
import {COMPUTER_LANGUAGES, mapLanguageToDisplayName} from "../education/programmingcontestFE/Constant";
import {errorNoti} from "../../utils/notification";
import {toFormattedDateTime} from "../../utils/dateutils";
import {DateTimePicker} from "@mui/x-date-pickers";
import {LocalizationProvider} from "@mui/x-date-pickers/LocalizationProvider";
import {AdapterMoment} from "@mui/x-date-pickers/AdapterMoment";

const filterInitValue = {
  userId: "",
  contestId: "",
  problemId: "",
  statuses: [],
  languages: [],
  fromDate: null,
  toDate: null,
}
const getStatuses = (t) => [
  {label: t('Accepted'), value: "Accepted"},
  {label: t('Compile Error'), value: "Compile Error"},
  {label: t('Evaluated'), value: "Evaluated"},
  {label: t('Failed'), value: "Failed"},
  {label: t('In progress'), value: "In progress"},
  {label: t('N/E Forbidden Ins.'), value: "N/E Forbidden Ins."},
  {label: t('Partial'), value: "Partial"},
  {label: t('Pending Evaluation'), value: "Pending Evaluation"}
];
const getLanguages = (t) => [
  COMPUTER_LANGUAGES.C,
  COMPUTER_LANGUAGES.CPP11,
  COMPUTER_LANGUAGES.CPP14,
  COMPUTER_LANGUAGES.CPP17,
  COMPUTER_LANGUAGES.JAVA,
  COMPUTER_LANGUAGES.PYTHON
].map(language => ({
  label: t(mapLanguageToDisplayName(language)),
  value: language,
}))

const ViewProgrammingContestSubmission = () => {
  const {t} = useTranslation(["education/programmingcontest/problem", "education/programmingcontest/contest", "education/programmingcontest/testcase", "common"]);

  const languages = getLanguages(t);
  const statuses = getStatuses(t)

  const [submissions, setSubmissions] = useState([]);
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(5);
  const [totalCount, setTotalCount] = useState(0);
  const [filter, setFilter] = useState(filterInitValue);

  const columns = [
    {
      title: "ID",
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
      cellStyle: {
        minWidth: 140,
      },
      render: (rowData) => (
        <Tooltip title={rowData.fullName} placement="bottom-start" arrow>
          {rowData.userId}
        </Tooltip>
      ),
    },
    {
      title: t("common:id", {name: t('education/programmingcontest/contest:contest')}),
      field: "contestId",
      cellStyle: {
        minWidth: 110,
      },
    },
    {
      title: t("common:id", {name: t('education/programmingcontest/problem:problem')}),
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
      title: t("status"),
      cellStyle: {
        minWidth: 120,
      },
      render: (rowData) => (
        <span style={{color: getStatusColor(`${rowData.status}`)}}>
          {`${rowData.status}`}
        </span>
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
      title: t('common:language'),
      cellStyle: {
        minWidth: 100,
      },
      render: (rowData) => mapLanguageToDisplayName(rowData.sourceCodeLanguage)
    },
    {
      title: t("common:createdTime"),
      cellStyle: {minWidth: 130},
      render: (rowData) => toFormattedDateTime(rowData.createdAt)
    },
    {
      title: t("common:action"),
      align: "center",
      cellStyle: {minWidth: 100},
      render: (rowData) => <RejudgeButton submissionId={rowData.contestSubmissionId}/>
    },
  ];

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
    let url = `/admin/data/view-contest-submission?page=${page}&size=${pageSize}`;

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
        setLoading(false);

        const data = res.data
        const myProblems = data.content

        if (data.numberOfElements === 0 && data.number > 0) {
          setPage(0)
        } else {
          setSubmissions(myProblems);
          setTotalCount(data.totalElements)
        }
      },
      {
        onError: (e) => {
          setLoading(false);
          errorNoti(t("common:error", 3000))
        }
      });
  }

  useEffect(() => {
    handleSearch()
  }, [page, pageSize]);

  return (
    <Paper elevation={1} sx={{padding: "16px 24px", borderRadius: 4}} square={false}>
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
            label={t("common:id", {name: t('education/programmingcontest/contest:contest')})}
            value={filter.contestId}
            onChange={e => handleFilterChange('contestId', e)}
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
        <Grid item xs={3}>
          <StyledSelect
            fullWidth
            key={t("status")}
            label={t("status")}
            options={statuses}
            value={filter.statuses}
            sx={{minWidth: 'unset', mr: 'unset'}}
            SelectProps={selectProps(statuses)}
            onChange={e => handleFilterChange('statuses', e)}
          />
        </Grid>
        <Grid item xs={3}>
          <StyledSelect
            fullWidth
            key={t("common:language")}
            label={t("common:language")}
            options={languages}
            value={filter.languages}
            sx={{minWidth: 'unset', mr: 'unset'}}
            SelectProps={selectProps(languages)}
            onChange={e => handleFilterChange('languages', e)}
          />
        </Grid>
        <LocalizationProvider dateAdapter={AdapterMoment}>
          <Grid item xs={3}>
            <DateTimePicker
              ampm={false}
              label={`${t("common:createdTime")} - ${t("common:fromDate").toLowerCase()}`}
              value={filter.fromDate}
              onChange={(newValue) => handleDateChange("fromDate", newValue)}
              renderInput={(params) => <TextField {...params} size="small" fullWidth/>}
              inputFormat="YYYY-MM-DD HH:mm"
            />
          </Grid>
          <Grid item xs={3}>
            <DateTimePicker
              ampm={false}
              label={`${t("common:createdTime")} - ${t("common:toDate").toLowerCase()}`}
              value={filter.toDate}
              onChange={(newValue) => handleDateChange("toDate", newValue)}
              renderInput={(params) => <TextField {...params} size="small" fullWidth/>}
              inputFormat="YYYY-MM-DD HH:mm"
            />
          </Grid>
        </LocalizationProvider>
      </Grid>
      <Stack direction="row" justifyContent='flex-end' spacing={2} sx={{mt: 3}}>
        <TertiaryButton
          onClick={resetFilter}
          variant="outlined"
          startIcon={<AutorenewIcon/>}
        >
          {t("reset")}
        </TertiaryButton>
        <PrimaryButton
          disabled={loading}
          onClick={handleSearch}
          startIcon={<SearchIcon/>}
        >
          {t("search")}
        </PrimaryButton>
      </Stack>

      <Divider sx={{mt: 2, mb: 2}}/>

      <Stack direction="row" justifyContent='space-between' mb={1.5}>
        <Typography variant="h6">{t("education/programmingcontest/contest:submissionList")}</Typography>
      </Stack>
      <StandardTable
        hideCommandBar
        hideToolBar
        columns={columns}
        data={submissions}
        options={{
          pageSize: pageSize,
          selection: false,
          search: false,
          sorting: false
        }}
        components={{
          Container: (props) => <Paper {...props} elevation={0}/>,
        }}
        isLoading={loading}
        page={page}
        totalCount={totalCount}
        onChangePage={handleChangePage}
        onChangeRowsPerPage={handleChangePageSize}
      />
    </Paper>
  );
}

const screenName = "SCR_ADMIN_CONTEST_SUBMISSION";
export default withScreenSecurity(
  ViewProgrammingContestSubmission,
  screenName,
  true
);
