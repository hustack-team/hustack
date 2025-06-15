import React, {useEffect, useState} from 'react';
import withScreenSecurity from "../../../withScreenSecurity";
import {
  Accordion, AccordionDetails, AccordionSummary,
  Box,
  Button,
} from "@material-ui/core";
import {formatDateTime, getDiffMinutes} from "../ultils/DateUltils";
import {request} from "../../../../api";
import TestBankDetails from "../testbank/TestBankDetails";
import {DataGrid} from "@material-ui/data-grid";
import ExamMarking from "./ExamMarking";
import {parseHTMLToString} from "../ultils/DataUltils";
import {ExpandMore} from "@material-ui/icons";
import CustomizedDialogs from "../../../dialog/CustomizedDialogs";
import {makeStyles} from "@material-ui/core/styles";
import PrimaryButton from "../../../button/PrimaryButton";
import TertiaryButton from "../../../button/TertiaryButton";
import ExamViolateDialog from "./ExamViolateDialog";
import SecondaryButton from "../ultils/component/SecondaryButton";
import {errorNoti, successNoti} from "../../../../utils/notification";
import ExamContinueDoingDialog from "./ExamContinueDoingDialog";

const baseColumn = {
  sortable: false,
};

const useStyles = makeStyles((theme) => ({
  dialogContent: {minWidth: '90vw'},
}));

function ExamDetails(props) {

  const columns = [
    {
      field: "code",
      headerName: "Mã học viên",
      minWidth: 140,
      ...baseColumn
    },
    {
      field: "name",
      headerName: "Họ và tên",
      minWidth: 160,
      flex: 1,
      ...baseColumn
    },
    {
      field: "email",
      headerName: "Email",
      ...baseColumn,
      flex: 1,
      minWidth: 160,
    },
    {
      field: "phone",
      headerName: "Số điện thoại",
      ...baseColumn,
      minWidth: 120,
    },
    {
      field: "totalScore",
      headerName: "Điểm",
      ...baseColumn,
      minWidth: 60,
      maxWidth: 80,
    },
    {
      field: "totalTime",
      headerName: "Thời gian làm",
      renderCell: (rowData) => {
        if(rowData?.row?.submitedAt){
          return `${getDiffMinutes(rowData?.row?.startedAt, rowData?.row?.submitedAt)} Phút`
        }
        return ''
      },
      ...baseColumn,
      minWidth: 120,
    },
    {
      field: "totalViolate",
      headerName: "Lỗi vi phạm",
      renderCell: (rowData) => {
        if(rowData?.value){
          return (
            <p
              style={{fontWeight: 'bolder', cursor: 'pointer', textDecoration: 'underline', color: 'red'}}
              onClick={() => handleOpenPopupExamViolate(rowData?.row?.examResultId)}
            >
              {String(rowData?.value).padStart(2, '0')} lỗi
            </p>
          )
        }
        return ''
      },
      ...baseColumn,
      minWidth: 100,
      maxWidth: 120,
    },
    {
      field: "",
      headerName: "",
      sortable: false,
      minWidth: 240,
      renderCell: (rowData) => {
        return (
          <Box display="flex" justifyContent="end" alignItems='center' width="100%">
            {
              rowData?.row?.examResultId ? (
                <div style={{display: "flex", gap: 12, width: 'inherit'}}>
                  {
                    !rowData?.row?.submitedAt && !rowData?.row?.submitAgain && (
                      <SecondaryButton
                        variant="outlined"
                        onClick={(data) => {
                          setOpenExamContinueDoingDialog(true)
                          setExamStudentTestSelected(rowData?.row)
                        }}
                      >
                        Mở làm tiếp
                      </SecondaryButton>
                    )
                  }
                  <PrimaryButton
                    variant="contained"
                    color="primary"
                    onClick={(data) => handleMarking(rowData?.row)}
                    style={{width: !(!rowData?.row?.submitedAt && !rowData?.row?.submitAgain) ? '100%' : ''}}
                  >
                    Chấm điểm
                  </PrimaryButton>
                </div>
              ) : (
                <TertiaryButton
                  variant="outlined"
                  color="secondary"
                  style={{pointerEvents: "none", width: '100%'}}
                >
                  Chưa làm
                </TertiaryButton>
              )
            }
          </Box>
        )
      }
    },
  ];

  const classes = useStyles();
  const { open, setOpen, dataExam} = props;

  const [rowData, setRowData] = useState([])
  const [examExamTestIdFocus, setExamExamTestIdFocus] = useState(null)
  const [data, setData] = useState(dataExam)
  const [openTestDetailsDialog, setOpenTestDetailsDialog] = useState(false);
  const [testDetails, setTestDetails] = useState(null)
  const [openExamDetailsMarkingDialog, setOpenExamDetailsMarkingDialog] = useState(false);
  const [examDetailsMarking, setExamDetailsMarking] = useState(null)
  const [expanded, setExpanded] = useState(false)
  const [openExamViolateDialog, setOpenExamViolateDialog] = useState(false);
  const [examResultIdViolate, setExamResultIdViolate] = useState(null);
  const [openExamContinueDoingDialog, setOpenExamContinueDoingDialog] = useState(false);
  const [examStudentTestSelected, setExamStudentTestSelected] = useState(null);

  const handleOpenPopupTestDetails = (test) =>{
    request(
      "get",
      `/exam-test/${test.id}`,
      (res) => {
        if(res.data.resultCode === 200){
          setTestDetails(res.data.data)
          setOpenTestDetailsDialog(true)
        }else{
          errorNoti(res.data.resultMsg, 3000)
        }
      },
      { onError: (e) => errorNoti(e, 3000) },
    );
  }

  const handleOpenPopupExamViolate = (examResultId) => {
    setOpenExamViolateDialog(true)
    setExamResultIdViolate(examResultId)
  }

  const closeDialog = () => {
    setOpen(false)
  }

  const handleUpdateExamResult = (rowData) =>{
    const body = {
      examStudentTestId: rowData?.examStudentTestId,
      submitAgain: true,
    }
    request(
      "put",
      `/exam-result`,
      (res) => {
        if(res.data.resultCode === 200){
          successNoti(res.data.resultMsg, 3000)
          handleFetchListStudentExam(examExamTestIdFocus)
        }else{
          errorNoti(res.data.resultMsg, 3000)
        }
      },
      { onError: (e) => errorNoti(e, 3000) },
      body,
    );
  }

  const handleMarking = (rowData) => {
    request(
      "get",
      `/exam/teacher/submissions/${rowData?.examStudentTestId}`,
      (res) => {
        if(res.data.resultCode === 200){
          setExamDetailsMarking(res.data.data)
          setOpenExamDetailsMarkingDialog(true)
          setExpanded(false)
        }else{
          errorNoti(res.data.resultMsg, 3000)
        }
      },
      { onError: (e) => errorNoti(e, 3000) }
    );
  }

  const handleChangeAccordion = (test, panel) => (event, isExpanded) => {
    setExpanded(isExpanded ? panel : false);
    if(isExpanded){
      setExamExamTestIdFocus(test.examExamTestId)
      handleFetchListStudentExam(test.examExamTestId)
    }
  }

  const handleFetchListStudentExam = (examExamTestId) => {
    request(
      "get",
      `exam/examTest/${examExamTestId}`,
      (res) => {
        if(res.data.resultCode === 200){
          setRowData(res.data.data)
        }else{
          errorNoti(res.data.resultMsg, 3000)
        }
      },
      { onError: (e) => errorNoti(e, 3000) },
    );
  }

  return (
    <div>
      <CustomizedDialogs
        open={open}
        classNames={{paper: classes.dialogContent}}
        handleClose={closeDialog}
        title={data?.name}
        content={
          <div>
            <div style={{display: "flex", justifyContent: 'space-between'}}>
              <div style={{display: "flex"}}>
                <h4 style={{margin: '0 5px 0 0', padding: 0}}>Bắt đầu từ:</h4>
                <span>{formatDateTime(data?.startTime)} - {formatDateTime(data?.endTime)}</span>
              </div>
            </div>

            <div style={{display: "flex", justifyContent: 'space-between'}}>
              <div style={{display: "flex", width: '30%'}}>
                <h4 style={{margin: '0 5px 0 0', padding: 0}}>Mã kỳ thi:</h4>
                <span>{data?.code}</span>
              </div>
              <div style={{display: "flex", width: '30%'}}>
                <h4 style={{margin: '0 5px 0 0', padding: 0}}>Trạng thái:</h4>
                <span>{data?.status === 0 ? 'Chưa kích hoạt' : 'Kích hoạt'}</span>
              </div>
              <div style={{display: "flex", width: '30%'}}>
                <h4 style={{margin: '0 5px 0 0', padding: 0}}>Trạng thái đáp án:</h4>
                <span>{data?.answerStatus === 'NO_OPEN' ? 'Ẩn' : 'Hiện'}</span>
              </div>
            </div>

            <div style={{display: "flex", justifyContent: 'space-between'}}>
              <div style={{display: "flex", width: '30%'}}>
                <h4 style={{margin: '0 5px 0 0', padding: 0}}>Trạng thái điểm:</h4>
                <span>{data?.scoreStatus === 0 ? 'Ẩn' : 'Công bố'}</span>
              </div>
              <div style={{display: "flex", width: '30%'}}>
                <h4 style={{margin: '0 5px 0 0', padding: 0}}>Hình thức giám sát:</h4>
                <span>
                  {{
                    0: 'Không giám sát',
                    1: 'Thao tác trình duyệt',
                    2: 'Thao tác trình duyệt + Camera'
                  }[data?.monitor] || 'Không giám sát'}
                </span>
              </div>
              {
                (data?.monitor === 1 || data?.monitor === 2) && (
                  <div style={{display: "flex", width: '30%'}}>
                    <h4 style={{margin: '0 5px 0 0', padding: 0}}>Khoá màn hình khi vi phạm:</h4>
                    <span>{data?.blockScreen === 0 ? 'Không khoá' : `${data?.blockScreen} giây`}</span>
                  </div>
                )
              }
            </div>

            <div style={{display: "flex", flexDirection: "column"}}>
              <h4 style={{margin: '15px 5px 0 0', padding: 0}}>Mô tả kỳ thi:</h4>
              <p style={{margin: 0, padding: 0}}>{parseHTMLToString(data?.description)}</p>
            </div>

            <div style={{display: "flex", flexDirection: "column"}}>
              <h4 style={{margin: '15px 5px 0 0', padding: 0}}>Đề thi:</h4>
              {
                data?.examTests.map((test, index) => {
                  return (
                    <Accordion
                      expanded={expanded === index}
                      onChange={handleChangeAccordion(test, index)}
                      sx={{
                        border: '1px solid #ddd',
                        borderRadius: 2,
                        boxShadow: '0 2px 4px rgba(0, 0, 0, 0.05)',
                        mb: 2,
                        '&:before': {display: 'none'},
                      }}
                    >
                      <AccordionSummary
                        expandIcon={<ExpandMore/>}
                        aria-controls={`panel${index}-content`}
                        id={`panel${index}-header`}
                        style={{flexDirection: 'row-reverse', paddingLeft: 0}}
                      >
                        <Box display="flex" alignItems="center" width="100%" justifyContent="space-between">
                          <Box display="flex" alignItems="center" width="calc(100% - 90px)">
                            <Box display="flex"
                                 flexDirection='column'
                                 width="100%"
                                 style={{
                                   userSelect: "none",
                                   WebkitUserSelect: "none",
                                   MozUserSelect: "none",
                                   msUserSelect: "none"
                                 }}>
                              <div style={{display: 'flex'}}>
                                <span style={{fontStyle: 'italic', marginRight: '5px'}}>({test?.code})</span>
                                <span style={{display: "block", fontWeight: 'bold'}}>{test?.name}</span>
                              </div>
                              {
                                test?.duration && (
                                  <p style={{margin: '0'}}><strong>Thời gian làm:</strong> {test?.duration} phút</p>
                                )
                              }
                              {
                                test?.description && test?.description !== '' && (
                                  <>{parseHTMLToString(test?.description)}</>
                                )
                              }
                            </Box>
                          </Box>

                          <button
                            style={{
                              height: 'max-content',
                              width: '80px',
                              padding: '8px',
                              border: 'none',
                              borderRadius: '8px',
                              cursor: 'pointer',
                              fontWeight: 'bold'
                            }}
                            onClick={(event) => {
                              handleOpenPopupTestDetails(test)
                              event.preventDefault()
                              event.stopPropagation()
                            }}>
                            Chi tiết
                          </button>
                        </Box>
                      </AccordionSummary>
                      <AccordionDetails>
                        <div style={{width: '100%'}}>
                          <h4 style={{margin: '15px 5px 0 0', padding: 0}}>Danh sách học viên:</h4>
                          <DataGrid
                            rows={rowData}
                            columns={columns}
                            getRowId={(row) => row.code}
                            disableColumnMenu
                            autoHeight
                          />
                        </div>
                      </AccordionDetails>
                    </Accordion>
                  )
                })
              }
            </div>
          </div>
        }
        actions={
          <TertiaryButton
            variant="outlined"
            onClick={closeDialog}
          >
            Hủy
          </TertiaryButton>
        }
      />
      {
        openTestDetailsDialog && (
          <TestBankDetails
            open={openTestDetailsDialog}
            setOpen={setOpenTestDetailsDialog}
            data={testDetails}
          />
        )
      }
      {
        openExamDetailsMarkingDialog && (
          <ExamMarking
            open={openExamDetailsMarkingDialog}
            setOpen={setOpenExamDetailsMarkingDialog}
            data={examDetailsMarking}
            setDataDetails={setData}
          />
        )
      }
      {
        openExamViolateDialog && (
          <ExamViolateDialog
            open={openExamViolateDialog}
            setOpen={setOpenExamViolateDialog}
            examResultId={examResultIdViolate}
          />
        )
      }
      {
        openExamContinueDoingDialog && (
          <ExamContinueDoingDialog
            open={openExamContinueDoingDialog}
            setOpen={setOpenExamContinueDoingDialog}
            onReload={() => handleFetchListStudentExam(examExamTestIdFocus)}
            examStudentTest={examStudentTestSelected}
          />
        )
      }
    </div>
  );
}

const screenName = "MENU_EXAM_MANAGEMENT";
// export default withScreenSecurity(QuestionBank, screenName, true);
export default ExamDetails;
