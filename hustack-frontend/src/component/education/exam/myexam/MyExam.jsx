import React, {useEffect, useState} from 'react';
import withScreenSecurity from "../../../withScreenSecurity";
import {
  Box,
  Card,
  CardContent,
  CardHeader,
  MenuItem,
  TextField
} from "@material-ui/core";
import {request} from "../../../../api";
import {useHistory} from "react-router-dom";
import useDebounceValue from "../hooks/use-debounce";
import {toast} from "react-toastify";
import {DataGrid} from "@mui/x-data-grid";
import {formatDateTime} from "../ultils/DateUltils";
import { BorderColor } from "@mui/icons-material";
import {parseHTMLToString} from "../ultils/DataUltils";
import PrimaryButton from "../../../button/PrimaryButton";

const baseColumn = {
  sortable: false,
};

const rowsPerPage = [5, 10, 20];

function MyExam(props) {

  const columns = [
    {
      field: "examName",
      headerName: "Kỳ thi",
      minWidth: 200,
      flex: 1,
      ...baseColumn
    },
    {
      field: "examDescription",
      headerName: "Mô tả",
      minWidth: 200,
      flex: 1,
      renderCell: (rowData) => {
        return parseHTMLToString(rowData.value)
      },
      ...baseColumn
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
    // {
    //   field: "examResultId",
    //   headerName: "Trạng thái",
    //   ...baseColumn,
    //   minWidth: 170,
    //   height: 20,
    //   renderCell: (rowData) => {
    //     if(rowData.row?.examResultId != null && rowData.row?.totalScore == null ){
    //       return (
    //         <strong style={{color: '#716DF2'}}>Chưa chấm</strong>
    //       )
    //     }else if(rowData.row?.examResultId != null && rowData.row?.totalScore != null ){
    //       return (
    //         <strong style={{color: '#61bd6d'}}>Đã chấm</strong>
    //       )
    //     }else{
    //       return (
    //         <strong style={{color: '#f50000c9'}}>Chưa làm</strong>
    //       )
    //     }
    //   },
    // },
    {
      field: "",
      headerName: "",
      sortable: false,
      minWidth: 100,
      maxWidth: 100,
      renderCell: (rowData) => {
        return (
          <Box display="flex" justifyContent="space-between" alignItems='center' width="100%">
            <PrimaryButton
              variant="outlined"
              color="primary"
              onClick={(data) => handleGetListExamTest(rowData?.row)}
            >
              Chi tiết
            </PrimaryButton>
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
      name: 'Chưa làm'
    },
    {
      value: 1,
      name: 'Chưa chấm'
    },
    {
      value: 2,
      name: 'Đã chấm'
    }
  ]

  const [dataList, setDataList] = useState([])
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(5)
  const [totalCount, setTotalCount] = useState(0)
  const [keywordFilter, setKeywordFilter] = useState("")
  const [statusFilter, setStatusFilter] = useState('all')

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
      `/exam/student/submissions?${queryParams}`,
      (res) => {
        if(res.status === 200){
          setDataList(res.data.content);
          setTotalCount(res.data.totalElements);
        }else {
          toast.error(res)
        }
      },
      { onError: (e) => toast.error(e) },
    );
  }

  const handleGetListExamTest = (rowData) => {
    request(
      "get",
      `/exam/student/submissions/examTest/${rowData?.examTestIds.join(',')}`,
      (res) => {
        if(res.status === 200){
          if(res.data.resultCode === 200){
            history.push({
              pathname: `/exam/my-exam-test`,
              state: {
                data: res.data?.data,
                exam: rowData
              },
            });
          }else{
            toast.error(res.data.resultMsg)
          }
        }else {
          toast.error(res)
        }
      },
      { onError: (e) => toast.error(e) },
    );
  };

  const handleDoingExam = (rowData) => {
    request(
      "get",
      `/exam/student/submissions/${rowData?.examId}/${rowData?.examStudentId}`,
      (res) => {
        if(res.status === 200){
          if(res.data.resultCode === 200){
            history.push({
              pathname: `/exam/doing`,
              state: {
                data: res.data.data
              },
            });
          }else{
            toast.error(res.data.resultMsg)
          }
        }else {
          toast.error(res)
        }
      },
      { onError: (e) => toast.error(e) },
    );
  };

  return (
    <div>
      <Card elevation={5} >
        <CardHeader
          title={
            <Box display="flex" justifyContent="space-between" alignItems="end" width="100%">
              <Box display="flex" flexDirection="column" width="100%">
                <h4 style={{marginTop: 0, paddingTop: 0}}>Danh sách kỳ thi của tôi</h4>
                <Box display="flex" justifyContent="flex-start" width="100%">
                  <TextField
                    autoFocus
                    id="keywordMyExam"
                    label="Nội dung tìm kiếm"
                    placeholder="Tìm kiếm theo tên hoặc mô tả kỳ thi"
                    value={keywordFilter}
                    style={{ width: "400px", marginRight: "16px"}}
                    size="small"
                    onChange={(event) => {
                      setKeywordFilter(event.target.value);
                    }}
                    InputLabelProps={{
                      shrink: true,
                    }}
                  />

                  {/*<TextField*/}
                  {/*  id="statusMyExam"*/}
                  {/*  select*/}
                  {/*  label="Trạng thái"*/}
                  {/*  style={{ width: "150px"}}*/}
                  {/*  size="small"*/}
                  {/*  value={statusFilter}*/}
                  {/*  onChange={(event) => {*/}
                  {/*    setStatusFilter(event.target.value);*/}
                  {/*  }}*/}
                  {/*>*/}
                  {/*  {*/}
                  {/*    statusList.map(item => {*/}
                  {/*      return (*/}
                  {/*        <MenuItem value={item.value}>{item.name}</MenuItem>*/}
                  {/*      )*/}
                  {/*    })*/}
                  {/*  }*/}
                  {/*</TextField>*/}
                </Box>
              </Box>
            </Box>
          }/>
        <CardContent>
          <DataGrid
            rowCount={totalCount}
            rows={dataList}
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
            getRowId={(row) => row.examId}
          />
        </CardContent>
      </Card>
    </div>
  );
}

const screenName = "MENU_EXAMINEE_PARTICIPANT";
export default withScreenSecurity(MyExam, screenName, true);
//export default MyExam;
