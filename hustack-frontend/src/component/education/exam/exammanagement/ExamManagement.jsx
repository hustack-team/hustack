import React, {useEffect, useState} from 'react';
import withScreenSecurity from "../../../withScreenSecurity";
import {Box, Button, Card, CardContent, CardHeader, Input} from "@material-ui/core";
import AddCircleIcon from "@material-ui/icons/AddCircle";
import {request} from "../../../../api";
import {Link, useHistory} from "react-router-dom";
import {FormControl, MenuItem, Select} from "@mui/material";
import useDebounceValue from "../hooks/use-debounce";
import {toast} from "react-toastify";
import TextField from "@material-ui/core/TextField";
import {DataGrid} from "@material-ui/data-grid";
import InfoIcon from "@mui/icons-material/Info";
import EditIcon from "@material-ui/icons/Edit";
import DeleteIcon from "@material-ui/icons/Delete";
import {formatDateTime} from "../ultils/DateUltils";
import ExamDelete from "./ExamDelete";
import ExamDetails from "./ExamDetails";
import {parseHTMLToString} from "../ultils/DataUltils";
import PrimaryButton from "../../../button/PrimaryButton";

const baseColumn = {
  sortable: false,
  headerClassName: 'wrap-header',
};

const rowsPerPage = [5, 10, 20];

function ExamManagement(props) {

  const columns = [
    // {
    //   field: "code",
    //   headerName: "Mã kỳ thi",
    //   minWidth: 170,
    //   ...baseColumn
    // },
    {
      field: "name",
      headerName: "Tên kỳ thi",
      minWidth: 200,
      flex: 1,
      ...baseColumn
    },
    // {
    //   field: "description",
    //   headerName: "Mô tả kỳ thi",
    //   ...baseColumn,
    //   flex: 1,
    //   minWidth: 200,
    //   renderCell: (rowData) => {
    //     return parseHTMLToString(rowData.value)
    //   }
    // },
    {
      field: "status",
      headerName: "Trạng thái",
      ...baseColumn,
      minWidth: 120,
      renderCell: (rowData) => {
        if(rowData.value === 0){
          return (
            <strong style={{color: '#f50000c9'}}>Chưa kích hoạt</strong>
          )
        }else{
          return (
            <strong style={{color: '#61bd6d'}}>Kích hoạt</strong>
          )
        }
      },
    },
    {
      field: "answerStatus",
      headerName: "Đáp án",
      ...baseColumn,
      minWidth: 80,
      renderCell: (rowData) => {
        if(rowData.value === 'NO_OPEN'){
          return (
            <strong style={{color: '#f50000c9'}}>Ẩn</strong>
          )
        }else if(rowData.value === 'OPEN'){
          return (
            <strong style={{color: '#61bd6d'}}>Hiện</strong>
          )
        }else{
          return ''
        }
      },
    },
    {
      field: "scoreStatus",
      headerName: "Điểm",
      ...baseColumn,
      minWidth: 80,
      renderCell: (rowData) => {
        if(rowData.value === 0){
          return (
            <strong style={{color: '#f50000c9'}}>Ẩn</strong>
          )
        }else if(rowData.value === 1){
          return (
            <strong style={{color: '#61bd6d'}}>Công bố</strong>
          )
        }else{
          return ''
        }
      },
    },
    {
      field: "monitor",
      headerName: "Hình thức giám sát",
      ...baseColumn,
      minWidth: 160,
      flex: 1,
      renderCell: (rowData) => {
        if(rowData.value === 0){
          return (
            <strong style={{color: '#f50000c9'}}>Không giám sát</strong>
          )
        }else if(rowData.value === 1){
          return (
            <strong style={{color: '#61bd6d'}}>Thao tác trình duyệt</strong>
          )
        }else if(rowData.value === 2){
          return (
            <div style={{display: 'flex', flexDirection:'column', color: '#61bd6d'}}>
              <strong style={{lineHeight: "normal"}}>Thao tác trình duyệt</strong>
              <strong style={{lineHeight: "normal"}}>Camera</strong>
            </div>
          )
        }else{
          return ''
        }
      },
    },
    {
      field: "blockScreen",
      headerName: "Khoá màn hình khi vi phạm",
      ...baseColumn,
      minWidth: 150,
      flex: 1,
      renderCell: (rowData) => {
        if(rowData.value === 0){
          return 'Không khoá'
        }else if(rowData.value > 0){
          return `${rowData.value} giây`
        }else{
          return ''
        }
      },
    },
    {
      field: "startTime",
      headerName: "Thời gian bắt đầu",
      ...baseColumn,
      minWidth: 170,
      renderCell: (rowData) => {
        return formatDateTime(rowData.value)
      },
    },
    {
      field: "endTime",
      headerName: "Thời gian kết thúc",
      ...baseColumn,
      minWidth: 170,
      renderCell: (rowData) => {
        return formatDateTime(rowData.value)
      },
    },
    {
      field: "",
      headerName: "",
      sortable: false,
      minWidth: 120,
      maxWidth: 120,
      renderCell: (rowData) => {
        return (
          <Box display="flex" justifyContent="space-between" alignItems='center' width="100%">
            <InfoIcon style={{cursor: 'pointer'}} onClick={(data) => handleDetails(rowData?.row)}/>
            <EditIcon style={{cursor: 'pointer'}} onClick={(data) => handleUpdate(rowData?.row)}/>
            <DeleteIcon style={{cursor: 'pointer', color: 'red'}} onClick={(data) => handleDelete(rowData?.row)}/>
          </Box>
        )
      }
    },
  ];

  const statusList = [
    {
      value: 'all',
      name: 'Tất cả'
    },
    {
      value: 0,
      name: 'Chưa kích hoạt'
    },
    {
      value: 1,
      name: 'Kích hoạt'
    }
  ]

  const [examList, setExamList] = useState([])
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(5)
  const [totalCount, setTotalCount] = useState(0)
  const [keywordFilter, setKeywordFilter] = useState("")
  const [statusFilter, setStatusFilter] = useState('all')
  const [openDeleteDialog, setOpenDeleteDialog] = useState(false);
  const [idDelete, setIdDelete] = useState("")
  const [openDetailsDialog, setOpenDetailsDialog] = useState(false);
  const [examDetails, setExamDetails] = useState(null)

  const debouncedKeywordFilter = useDebounceValue(keywordFilter, 500)
  const history = useHistory();

  useEffect(() => {
    handleFilter()
  }, [page, pageSize, debouncedKeywordFilter, statusFilter]);

  const handleFilter = () =>{
    const queryParams = new URLSearchParams({
      page: page,
      size: pageSize,
      keyword: keywordFilter,
    })
    if (statusFilter != null && statusFilter !== "all") queryParams.append('status', statusFilter)
    request(
      "get",
      `/exam?${queryParams}`,
      (res) => {
        if(res.status === 200){
          setExamList(res.data.content);
          setTotalCount(res.data.totalElements);
        }else {
          toast.error(res)
        }
      },
      { onError: (e) => toast.error(e) },
    );
  }

  const onClickCreateNewButton = () => {
    history.push({
      pathname: "/exam/create-update",
      state: {
        data: {
          examTestId: "",
          examTests: "",
          code: "",
          name: "",
          description: "",
          status: 1,
          monitor: 0,
          blockScreen: 0,
          timeBlockScreen: null,
          answerStatus: "NO_OPEN",
          scoreStatus: 0,
          startTime: "",
          endTime: "",
          examStudents: []
        },
        isCreate: true
      },
    });
  };

  const handleUpdate = (rowData) => {
    request(
      "get",
      `/exam/preview-update/${rowData.id}`,
      (res) => {
        if(res.data.resultCode === 200){
          history.push({
            pathname: "/exam/create-update",
            state: {
              data: res.data.data,
              isCreate: false
            },
          });
        }else{
          toast.error(res.data.resultMsg)
        }
      },
      { onError: (e) => toast.error(e) },
    );
  };

  const handleDetails = (rowData) => {
    request(
      "get",
      `/exam/${rowData.id}`,
      (res) => {
        if(res.data.resultCode === 200){
          setExamDetails(res.data.data)
          setOpenDetailsDialog(true)
        }else{
          toast.error(res.data.resultMsg)
        }
      },
      { onError: (e) => toast.error(e) },
    );
  };

  const handleDelete = (rowData) => {
    setOpenDeleteDialog(true)
    setIdDelete(rowData.id)
  };

  return (
    <div>
      <Card elevation={5} >
        <CardHeader
          title={
            <Box display="flex" justifyContent="space-between" alignItems="end" width="100%">
              <Box display="flex" flexDirection="column" width="80%">
                <h4 style={{marginTop: 0, paddingTop: 0}}>Danh sách kỳ thi</h4>
                <Box display="flex" justifyContent="flex-start" width="100%">
                  <TextField
                    autoFocus
                    id="examCode"
                    label="Nội dung tìm kiếm"
                    placeholder="Tìm kiếm theo code hoặc tên"
                    value={keywordFilter}
                    style={{ width: "300px", marginRight: "16px"}}
                    onChange={(event) => {
                      setKeywordFilter(event.target.value);
                    }}
                    InputLabelProps={{
                      shrink: true,
                    }}
                  />

                  <TextField
                    id="examStatus"
                    select
                    label="Trạng thái"
                    style={{ width: "150px"}}
                    value={statusFilter}
                    onChange={(event) => {
                      setStatusFilter(event.target.value);
                    }}
                  >
                    {
                      statusList.map(item => {
                        return (
                          <MenuItem value={item.value}>{item.name}</MenuItem>
                        )
                      })
                    }
                  </TextField>
                </Box>
              </Box>

              <Box display="flex" justifyContent="flex-end" width="20%">
                <PrimaryButton
                  variant="contained"
                  color="primary"
                  onClick={onClickCreateNewButton}
                  startIcon={<AddCircleIcon />}
                  style={{ marginRight: 16 }}
                >
                  Thêm mới
                </PrimaryButton>
              </Box>
            </Box>
          }/>
        <CardContent>
          <DataGrid
            rowCount={totalCount}
            rows={examList}
            columns={columns}
            page={page}
            pageSize={pageSize}
            pagination
            paginationMode="server"
            onPageChange={(page) => setPage(page)}
            onPageSizeChange={(pageSize) => setPageSize(pageSize)}
            rowsPerPageOptions={rowsPerPage}
            disableColumnMenu
            autoHeight
          />
        </CardContent>
      </Card>
      {
        openDetailsDialog && (
          <ExamDetails
            open={openDetailsDialog}
            setOpen={setOpenDetailsDialog}
            dataExam={examDetails}
          />
        )
      }
      <ExamDelete
        open={openDeleteDialog}
        setOpen={setOpenDeleteDialog}
        id={idDelete}
        onReload={() => {
          handleFilter()
        }}
      />
    </div>
  );
}

const screenName = "MENU_EXAM_MANAGEMENT";
export default withScreenSecurity(ExamManagement, screenName, true);
// export default ExamManagement;
