import React, {useEffect, useState} from 'react';
import withScreenSecurity from "../../../withScreenSecurity";
import {
  Accordion, AccordionDetails, AccordionSummary,
  Box,
  Button,
} from "@material-ui/core";
import {formatDateTime} from "../ultils/DateUltils";
import {request} from "../../../../api";
import {toast} from "react-toastify";
import TestBankDetails from "../testbank/TestBankDetails";
import {DataGrid} from "@material-ui/data-grid";
import ExamMarking from "./ExamMarking";
import {parseHTMLToString} from "../ultils/DataUltils";
import {Error, ExpandMore} from "@material-ui/icons";
import CustomizedDialogs from "../../../dialog/CustomizedDialogs";
import {makeStyles} from "@material-ui/core/styles";
import PrimaryButton from "../../../button/PrimaryButton";
import TertiaryButton from "../../../button/TertiaryButton";

const baseColumn = {
  sortable: false,
};

const useStyles = makeStyles((theme) => ({
  dialogContent: {minWidth: '90vw'},
}));

function ExamViolateDialog(props) {

  const columns = [
    {
      field: "platform",
      headerName: "Loại giám sát",
      minWidth: 180,
      renderCell: (rowData) => {
        if(rowData?.value === 0){
          return 'Giám sát màn hình'
        }else if(rowData?.value === 1){
          return 'Giám sát camera'
        }
        return ''
      },
      ...baseColumn
    },
    {
      field: "type",
      headerName: "Loại vi phạm",
      minWidth: 160,
      renderCell: (rowData) => {
        if(rowData?.value === 0){
          return 'Thoát màn hình'
        }else if(rowData?.value === 1){
          return 'Rời camera'
        }else if(rowData?.value === 2){
          return 'Có nhiều hơn 1 thí sinh'
        }
        return ''
      },
      ...baseColumn
    },
    {
      field: "startTime",
      headerName: "Thời gian bắt đầu vi phạm",
      ...baseColumn,
      renderCell: (rowData) => {
        if(rowData?.value){
          return formatDateTime(rowData?.value)
        }
        return ''
      },
      minWidth: 220,
    },
    {
      field: "toTime",
      headerName: "Thời gian kết thúc vi phạm",
      ...baseColumn,
      renderCell: (rowData) => {
        if(rowData?.value){
          return formatDateTime(rowData?.value)
        }
        return ''
      },
      minWidth: 220,
    },
    {
      field: "id",
      headerName: "Tổng thời gian vi phạm",
      ...baseColumn,
      renderCell: (rowData) => {
        if(rowData?.row.startTime && rowData?.row.toTime){
          return `${Math.round((new Date(rowData?.row.toTime) - new Date(rowData?.row.startTime)) / 1000)} giây`
        }
        return ''
      },
      minWidth: 200,
    },
    {
      field: "note",
      headerName: "Ghi chú",
      flex: 1,
      ...baseColumn,
      minWidth: 100,
    },
  ];

  const classes = useStyles();
  const { open, setOpen, examResultId} = props;

  const [rowData, setRowData] = useState([])

  useEffect(() => {
    request(
      "get",
      `/exam-monitor/result/${examResultId}`,
      (res) => {
        setRowData(res.data)
      },
      { onError: (e) => toast.error(e) },
    );
  }, [examResultId]);

  const closeDialog = () => {
    setOpen(false)
  }

  return (
    <div>
      <CustomizedDialogs
        open={open}
        classNames={{paper: classes.dialogContent}}
        handleClose={closeDialog}
        title={
          <div style={{display: 'flex', alignItems: 'center', color: 'red'}}>
            <Error/>
            <h3>Vi phạm quy chế thi</h3>
          </div>
        }
        content={
          <div style={{width: '100%'}}>
            <h4 style={{margin: '15px 5px 0 0', padding: 0}}>Danh sách vi phạm:</h4>
            <DataGrid
              rows={rowData}
              columns={columns}
              disableColumnMenu
              autoHeight
              hideFooter
            />
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
    </div>
  );
}

const screenName = "MENU_EXAM_MANAGEMENT";
// export default withScreenSecurity(QuestionBank, screenName, true);
export default ExamViolateDialog;
