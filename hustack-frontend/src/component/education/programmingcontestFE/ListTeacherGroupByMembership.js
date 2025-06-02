import { Box, Divider, Grid, Paper, Stack, TextField, Typography } from "@mui/material";
import React, { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import { request } from "api";
import { toFormattedDateTime } from "../../../utils/dateutils";
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

const filterInitValue = { keyword: "", status: "" };

function TeacherListGroup() {
  const { t } = useTranslation(["education/programmingcontest/group", "common"]);
  const [groups, setGroups] = useState([]);
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(5);
  const [totalElements, setTotalElements] = useState(0);
  const [filter, setFilter] = useState(filterInitValue);
  const [excludeIds, setExcludeIds] = useState([]);
  const [openDeleteDialog, setOpenDeleteDialog] = useState(false);
  const [selectedGroup, setSelectedGroup] = useState(null);

  // Static status options
  const statusOptions = [
    { value: "ACTIVE", label: t("common:statusActive") },
    { value: "INACTIVE", label: t("common:statusInactive") },
  ];

  const handleOpenDeleteDialog = (group) => {
    setSelectedGroup(group);
    setOpenDeleteDialog(true);
  };

  const handleCloseDeleteDialog = () => {
    setOpenDeleteDialog(false);
    setSelectedGroup(null);
  };

  const handleDeleteGroup = () => {
    if (selectedGroup) {
      request(
        "delete",
        `/groups/${selectedGroup.id}`,
        () => {
          setGroups(groups.filter((group) => group.id !== selectedGroup.id));
          setTotalElements((prev) => prev - 1);
          successNoti(t("common:deleteSuccess"), 3000);
        },
        {
          403: () => errorNoti(t("common:noPermission"), 3000),
          404: () => errorNoti(t("common:groupNotFound"), 3000),
        }
      ).then(() => {
        handleCloseDeleteDialog();
      }).catch((err) => {
        errorNoti(t("common:deleteFailed"), 3000);
        handleCloseDeleteDialog();
      });
    }
  };

  const handleChangeKeyword = (event) => {
    setFilter((prevFilter) => ({ ...prevFilter, keyword: event.target.value }));
  };

  const handleChangeStatus = (event) => {
    setFilter((prevFilter) => ({ ...prevFilter, status: event.target.value }));
  };

  const resetFilter = () => {
    setFilter(filterInitValue);
    setPage(0);
  };

  const handleChangePage = (newPage) => {
    setPage(newPage);
  };

  const handleChangePageSize = (newSize) => {
    setPage(0);
    setPageSize(newSize);
  };

  const handleSearch = () => {
    setLoading(true);
    let url = `/groups?page=${encodeURIComponent(page)}&size=${encodeURIComponent(pageSize)}`;
    
    // Only add keyword parameter if it has a value
    if (filter.keyword && filter.keyword.trim()) {
      url += `&keyword=${encodeURIComponent(filter.keyword)}`;
    }
    
    // Always send status parameter, even if empty
    url += `&status=${encodeURIComponent(filter.status)}`;
    
    if (excludeIds.length > 0) {
      url += `&exclude=${encodeURIComponent(excludeIds.join(","))}`;
    }

    console.log("Sending request to:", url);

    request(
      "get",
      url,
      (res) => {
        const data = res.data;
        console.log("Response data:", data);
        const groupsData = data.content || [];
        setGroups(groupsData);
        setTotalElements(data.totalElements);

        if (data.numberOfElements === 0 && data.number > 0) {
          setPage(0);
        }
      },
      {
        onError: (e) => {
          console.error("API error:", e);
          errorNoti(t("common:serverError"), 3000);
        },
      }
    ).then(() => setLoading(false));
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
          to={{
            pathname: `/programming-contest/group-manager/${rowData.id}`,
          }}
          style={{
            textDecoration: "none",
            color: "blue",
          }}
        >
          {rowData.name}
        </Link>
      ),
    },
    {
      title: t("common:status"),
      field: "status",
      cellStyle: { minWidth: 120 },
    },
    {
      title: t("common:description"),
      field: "description",
      cellStyle: { minWidth: 300 },
    },
    {
      title: t("common:createdBy"),
      field: "createdBy",
      cellStyle: { minWidth: 120 },
    },
    {
      title: t("common:createdTime"),
      field: "lastModifiedDate",
      cellStyle: { minWidth: 200 },
      render: (rowData) => toFormattedDateTime(rowData.lastModifiedDate),
    },
    {
      title: t("common:operation"),
      cellStyle: { minWidth: 120 },
      render: (rowData) => (
        <DeleteIcon
          style={{ cursor: "pointer", color: "#f44336" }}
          onClick={() => handleOpenDeleteDialog(rowData)}
        />
      ),
    },
  ];

  return (
    <Paper elevation={1} sx={{ padding: "16px 24px", borderRadius: 4 }}>
      <Typography variant="h6" sx={{ marginBottom: "12px" }}>
        {t("common:search")}
      </Typography>
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
        <Grid item xs={3}>
          <StyledSelect
            fullWidth
            key={t("common:status")}
            label={t("common:status")}
            options={statusOptions}
            value={filter.status}
            sx={{ minWidth: "unset", mr: "unset" }}
            onChange={handleChangeStatus}
          />
        </Grid>
        <Grid item xs={3} />
        <Grid item xs={3}>
          <Stack direction="row" spacing={2} justifyContent="flex-end">
            <TertiaryButton
              onClick={resetFilter}
              variant="outlined"
              startIcon={<AutorenewIcon />}
            >
              {t("common:reset")}
            </TertiaryButton>
            <PrimaryButton
              disabled={loading}
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
          onClick={() => {
            window.open("/programming-contest/group-form");
          }}
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
        isLoading={loading}
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