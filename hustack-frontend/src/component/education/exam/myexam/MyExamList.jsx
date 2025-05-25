import React from 'react';
import withScreenSecurity from "../../../withScreenSecurity";
import {
  Box,
  Card, CardActions,
  CardContent,
} from "@material-ui/core";
import {request} from "../../../../api";
import {useHistory} from "react-router-dom";
import {toast} from "react-toastify";
import {DataGrid} from "@mui/x-data-grid";
import {formatDateTime} from "../ultils/DateUltils";
import {parseHTMLToString} from "../ultils/DataUltils";
import PrimaryButton from "../../../button/PrimaryButton";
import SecondaryButton from "../ultils/component/SecondaryButton";
import {useLocation} from "react-router";
import TertiaryButton from "../../../button/TertiaryButton";
import {useMenu} from "../../../../layout/sidebar/context/MenuContext";

const baseColumn = {
  sortable: false,
};

function MyExamList(props) {

  const columns = [
    {
      field: "examTestName",
      headerName: "Đề thi",
      minWidth: 200,
      flex: 1,
      ...baseColumn
    },
    {
      field: "examTestDescription",
      headerName: "Mô tả",
      minWidth: 200,
      flex: 1,
      renderCell: (rowData) => {
        return parseHTMLToString(rowData.value)
      },
      ...baseColumn
    },
    {
      field: "totalScore",
      headerName: "Tổng điểm",
      ...baseColumn,
      minWidth: 170,
      maxWidth: 170,
    },
    {
      field: "totalTime",
      headerName: "Tổng thời gian làm",
      ...baseColumn,
      minWidth: 170,
    },
    {
      field: "",
      headerName: "",
      sortable: false,
      minWidth: 100,
      maxWidth: 100,
      renderCell: (rowData) => {
        return (
          <Box display="flex" justifyContent="space-between" alignItems='center' width="100%">
            {
              rowData?.row?.totalScore == null && rowData?.row?.totalTime == null ? (
                <PrimaryButton
                  variant="contained"
                  color="primary"
                  onClick={(data) => handleDoingExam(rowData?.row)}
                >
                  Làm bài
                </PrimaryButton>
              ) : (
                <SecondaryButton
                  variant="outlined"
                  onClick={(data) => handleDoingExam(rowData?.row)}
                >
                  Xem bài
                </SecondaryButton>
              )
            }
          </Box>
        )
      }
    },
  ];
  const history = useHistory();
  const location = useLocation();
  const data = location.state?.data
  const exam = location.state?.exam
  const { closeMenu, openMenu } = useMenu();

  if(data === undefined){
    window.location.href = '/exam/my-exam';
    openMenu()
  }

  const handleDoingExam = (rowData) => {
    closeMenu()
    if(exam?.examMonitor === 1 ||
      exam?.examMonitor === 2){
      history.push({
        pathname: `/exam/my-exam-preview`,
        state: {
          test: rowData,
          exam,
        },
      });
    }else{
      request(
        "get",
        `/exam/student/submissions/examStudentTest/${rowData?.examStudentTestId}`,
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
    }
  };

  return (
    <div>
      <Card elevation={5} >
        <CardContent>
          <div style={{display: "flex", flexDirection: "column", alignItems: 'center', width: '100%'}}>
            <h1 style={{margin: 0, padding: 0}}>{exam?.examName}</h1>
            <p style={{margin: 0, padding: 0}}>{parseHTMLToString(exam?.examDescription)}</p>
            <div style={{display: "flex"}}>
              <p style={{margin: '0 20px 0 0', padding: 0, display: "flex"}}><span style={{
                fontWeight: "bold",
                marginRight: '5px'
              }}>Thời gian bắt đầu:</span>{formatDateTime(exam?.startTime)}</p>
              <p style={{margin: 0, padding: 0, display: "flex"}}><span style={{
                fontWeight: "bold",
                marginRight: '5px'
              }}>Thời gian kết thúc:</span>{formatDateTime(exam?.endTime)}</p>
            </div>
          </div>
          <h3>Danh sách đề thi</h3>
          <DataGrid
            rows={data}
            columns={columns}
            getRowId={(row) => row.examTestId}
            disableColumnMenu
            autoHeight
            hideFooter
          />
        </CardContent>
        <CardActions style={{justifyContent: 'flex-end'}}>
          <TertiaryButton
            variant="outlined"
            onClick={() => {
              history.push("/exam/my-exam");
              openMenu()
            }}
          >
            Hủy
          </TertiaryButton>
        </CardActions>
      </Card>
    </div>
  );
}

const screenName = "MENU_EXAMINEE_PARTICIPANT";
export default withScreenSecurity(MyExamList, screenName, true);
//export default MyExam;
