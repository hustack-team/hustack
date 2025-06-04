import { Box, Divider, Grid, Paper, Stack, TextField, Typography, Tooltip } from "@mui/material";
import React, { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import { request } from "api";
import { errorNoti, successNoti } from "utils/notification";
import StandardTable from "../../table/StandardTable";
import PrimaryButton from "../../button/PrimaryButton";
import TertiaryButton from "../../button/TertiaryButton";
import SearchIcon from "@mui/icons-material/Search";
import AutorenewIcon from "@mui/icons-material/Autorenew";
import AddIcon from "@material-ui/icons/Add";
import DeleteIcon from "@material-ui/icons/Delete";
import { ConfirmDeleteDialog } from "component/dialog/ConfirmDeleteDialog";
import StyledSelect from "../../select/StyledSelect";
import { IconButton } from "@mui/material";
import { LockOpen, LockOutlined } from "@material-ui/icons";
import { width } from "@mui/system";

const INITIAL_FILTER = { keyword: "" };

function TeacherListGroup() {
  const { t } = useTranslation(["education/programmingcontest/group", "common"]);
  const [groups, setGroups] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [toggleLoading, setToggleLoading] = useState({});
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(5);
  const [totalElements, setTotalElements] = useState(0);
  const [filter, setFilter] = useState(INITIAL_FILTER);
  const [excludeIds, setExcludeIds] = useState([]);
  const [openDeleteDialog, setOpenDeleteDialog] = useState(false);
  const [selectedGroup, setSelectedGroup] = useState(null);

  const handleOpenDeleteDialog = (group) => {
    setSelectedGroup(group);
    setOpenDeleteDialog(true);
  };

  const handleCloseDeleteDialog = () => {
    setOpenDeleteDialog(false);
    setSelectedGroup(null);
  };

  const handleDeleteGroup = () => {
    if (!selectedGroup) return;

    setIsLoading(true);
    request(
      "delete",
      `/groups/${selectedGroup.id}`,
      () => {
        setGroups((prevGroups) => prevGroups.filter((g) => g.id !== selectedGroup.id));
        setTotalElements((prev) => prev - 1);
        successNoti(t("common:deleteSuccess"), 3000);
      },
      {
        403: () => errorNoti(t("common:noPermission"), 3000),
        404: () => errorNoti(t("common:groupNotFound"), 3000),
      }
    )
      .then(handleCloseDeleteDialog)
      .catch((err) => {
        errorNoti(t("common:deleteFailed"), 3000);
      })
      .finally(() => setIsLoading(false));
  };

  const handleChangeKeyword = (event) => {
    setFilter((prev) => ({ ...prev, keyword: event.target.value }));
  };

  const resetFilter = () => {
    setFilter(INITIAL_FILTER);
    setPage(0);
  };

  const handleChangePage = (newPage) => setPage(newPage);

  const handleChangePageSize = (newSize) => {
    setPage(0);
    setPageSize(newSize);
  };

  const handleSearch = () => {
    setIsLoading(true);
    const url = buildSearchUrl();

    request(
      "get",
      url,
      (res) => {
        const { data } = res;
        const groupsData = data.content || [];
        setGroups(groupsData);
        setTotalElements(data.totalElements);
        if (data.numberOfElements === 0 && data.number > 0) setPage(0);
      },
      {
        onError: (e) => {
          console.error("API error:", e);
          errorNoti(t("common:serverError"), 3000);
        },
      }
    ).finally(() => setIsLoading(false));
  };

  const buildSearchUrl = () => {
    let url = `/groups?page=${encodeURIComponent(page)}&size=${encodeURIComponent(pageSize)}`;
    if (filter.keyword?.trim()) url += `&keyword=${encodeURIComponent(filter.keyword)}`;
    if (excludeIds.length > 0) url += `&exclude=${encodeURIComponent(excludeIds.join(","))}`;
    return url;
  };

  useEffect(() => {
    handleSearch();
  }, [page, pageSize]);

  const columns = [
    {
      title: t("common:groupName"),
      field: "name",
      cellStyle: { minWidth: 300 },
      render: (rowData) => (
        <Link
          to={`/programming-contest/group-manager/${rowData.id}`}
          style={{ textDecoration: "none", color: "blue" }}
        >
          {rowData.name}
        </Link>
      ),
    },
    {
      title: t("common:description"),
      field: "description",
      cellStyle: { minWidth: 300 },
    },
    {
      title: t("common:action"),
      sorting: false,
      align: "center",
      width: 60,
      render: (rowData) => (
        <Stack direction="row" spacing={1} justifyContent="center">
          <Tooltip title={t("common:delete")} placement="top">
            <IconButton
              onClick={() => handleOpenDeleteDialog(rowData)}
              disabled={isLoading || toggleLoading[rowData.id]}
              color="error"
            >
              <DeleteIcon />
            </IconButton>
          </Tooltip>
        </Stack>
      ),
    },
  ];

  return (
    <Paper elevation={1} sx={{ padding: "16px 24px", borderRadius: 4 }}>
      <Typography variant="h6" sx={{ marginBottom: "12px" }}>
        {t("common:search")}
      </Typography>
      <Grid container spacing={3} alignItems="flex-end">
        <Grid item xs={12}>
          <Grid container spacing={3} alignItems="flex-end">
            <Grid item xs={3}>
              <TextField
                size="small"
                fullWidth
                label={t("common:groupName")}
                value={filter.keyword}
                onChange={handleChangeKeyword}
              />
            </Grid>
            <Grid item xs={6} />
          </Grid>
        </Grid>
        <Grid item xs={12}>
          <Stack direction="row" spacing={2} justifyContent="flex-end">
            <TertiaryButton
              onClick={resetFilter}
              variant="outlined"
              startIcon={<AutorenewIcon />}
            >
              {t("common:reset")}
            </TertiaryButton>
            <PrimaryButton
              disabled={isLoading}
              onClick={handleSearch}
              startIcon={<SearchIcon />}
            >
              {t("common:search")}
            </PrimaryButton>
          </Stack>
        </Grid>
      </Grid>

      <Divider sx={{ mt: 2, mb: 2 }} />

      <Stack direction="row" justifyContent="space-between" mb={1.5}>
        <Typography variant="h6">{t("common:groupList")}</Typography>
        <PrimaryButton
          startIcon={<AddIcon />}
          onClick={() => window.open("/programming-contest/group-form")}
        >
          {t("common:create", { name: "" })}
        </PrimaryButton>
      </Stack>
      <StandardTable
        columns={columns}
        data={groups}
        hideCommandBar
        hideToolBar
        options={{
          selection: false,
          pageSize,
          pageSizeOptions: [5, 10, 20],
          pagination: true,
          serverSide: true,
          totalCount: totalElements,
          page,
          search: false,
          sorting: false,
        }}
        components={{
          Container: (props) => <Paper {...props} elevation={0} />,
        }}
        isLoading={isLoading}
        page={page}
        totalCount={totalElements}
        onChangePage={handleChangePage}
        onChangeRowsPerPage={handleChangePageSize}
      />
      <ConfirmDeleteDialog
        open={openDeleteDialog}
        handleClose={handleCloseDeleteDialog}
        handleDelete={handleDeleteGroup}
        entity={t("education/programmingcontest/group:group")}
        name={selectedGroup?.name}
      />
    </Paper>
  );
}

export default TeacherListGroup;