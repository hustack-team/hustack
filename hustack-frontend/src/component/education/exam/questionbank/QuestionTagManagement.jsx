import React, {useEffect, useState} from 'react';
import withScreenSecurity from "../../../withScreenSecurity";
import {
  Box,
  Button, Card, CardContent, CardHeader, CircularProgress,
  Dialog,
  DialogContent,
  DialogTitle,
  Input
} from "@material-ui/core";
import {DialogActions, MenuItem} from "@mui/material";
import EditIcon from "@material-ui/icons/Edit";
import TextField from "@material-ui/core/TextField";
import AddCircleIcon from "@material-ui/icons/AddCircle";
import {DataGrid} from "@material-ui/data-grid";
import useDebounceValue from "../hooks/use-debounce";
import {request} from "../../../../api";
import {toast} from "react-toastify";
import QuestionTagCreateUpdate from "./QuestionTagCreateUpdate";
import CustomizedDialogs from "../../../dialog/CustomizedDialogs";
import {makeStyles} from "@material-ui/core/styles";

const baseColumn = {
  sortable: false,
};
const useStyles = makeStyles((theme) => ({
  dialogContent: {minWidth: 576},
}));
function QuestionTagManagement(props) {
  const classes = useStyles();
  const columns = [
    {
      field: "name",
      headerName: "Tên",
      flex: 1,
      minWidth: 170,
      ...baseColumn
    },
    {
      field: "",
      headerName: "",
      sortable: false,
      minWidth: 60,
      maxWidth: 60,
      renderCell: (rowData) => {
        return (
          <Box display="flex" justifyContent="space-between" alignItems='center' width="100%">
            <EditIcon style={{cursor: 'pointer'}} onClick={(data) => handleUpdate(rowData?.row)}/>
          </Box>
        )
      }
    },
  ];

  const [keywordFilter, setKeywordFilter] = useState("")
  const [questionTags, setQuestionTags] = useState([]);
  const [dataRowSelected, setDataRowSelected] = useState(null);
  const [openQuestionTagCreateUpdateDialog, setOpenQuestionTagCreateUpdateDialog] = useState(false);
  const [isCreateQuestionTag, setIsCreateQuestionTag] = useState(true);

  const debouncedKeywordFilter = useDebounceValue(keywordFilter, 500)

  const { open, setOpen, data} = props;

  useEffect(() => {
    setQuestionTags(data)
  }, [data]);

  useEffect(() => {
    console.log('keywordFilter',keywordFilter)
  }, [debouncedKeywordFilter]);

  useEffect(() => {
    if(!openQuestionTagCreateUpdateDialog){
      getAllQuestionTag()
    }
  }, [openQuestionTagCreateUpdateDialog]);

  const getAllQuestionTag = () => {
    request(
      "get",
      `/exam-tag`,
      (res) => {
        if(res.status === 200){
          setQuestionTags(res.data)
        }
      },
      { onError: (e) => toast.error(e) }
    );
  }

  const onClickCreateNewButton = () => {
    setOpenQuestionTagCreateUpdateDialog(true)
    setIsCreateQuestionTag(true)
    setDataRowSelected({
      name: ""
    })
  }

  const handleUpdate = (rowData) => {
    setOpenQuestionTagCreateUpdateDialog(true)
    setIsCreateQuestionTag(false)
    setDataRowSelected(rowData)
  }

  const closeDialog = () => {
    setOpen(false)
  }

  return (
    <div>
      <CustomizedDialogs
        open={open}
        handleClose={closeDialog}
        classNames={{paper: classes.dialogContent}}
        title='Danh sách Tag câu hỏi'
        content={
          <div>
            <Box display="flex" justifyContent="space-between" alignItems="end" width="100%" marginBottom="20px">
              <Box display="flex" flexDirection="column" width="70%">
                <Box display="flex" justifyContent="flex-start" width="100%">
                  <TextField
                    autoFocus
                    id="examQuestionTagName"
                    label="Nội dung tìm kiếm"
                    placeholder="Tìm kiếm theo tên"
                    value={keywordFilter}
                    style={{width: "300px", marginRight: "16px"}}
                    onChange={(event) => {
                      setKeywordFilter(event.target.value);
                    }}
                    InputLabelProps={{
                      shrink: true,
                    }}
                  />
                </Box>
              </Box>

              <Box display="flex" justifyContent="flex-end" width="30%">
                <Button
                  variant="contained"
                  color="primary"
                  onClick={onClickCreateNewButton}
                  startIcon={<AddCircleIcon/>}
                >
                  Thêm mới
                </Button>
              </Box>
            </Box>
            <DataGrid
              rows={questionTags}
              columns={columns}
              disableColumnMenu
              autoHeight
            />
            <QuestionTagCreateUpdate
              open={openQuestionTagCreateUpdateDialog}
              setOpen={setOpenQuestionTagCreateUpdateDialog}
              data={dataRowSelected}
              isCreate={isCreateQuestionTag}
            ></QuestionTagCreateUpdate>
          </div>
        }
        actions={
          <Button
            variant="contained"
            onClick={closeDialog}
          >
            Thoát
          </Button>
        }
      />
    </div>
  );
}

const screenName = "MENU_EXAM_QUESTION_BANK";
// export default withScreenSecurity(QuestionBank, screenName, true);
export default QuestionTagManagement;
