import React, {useEffect, useState} from 'react';
import withScreenSecurity from "../../../withScreenSecurity";
import {Box, Button, Card, CardContent, CardHeader, DialogTitle, Input} from "@material-ui/core";
import {request} from "../../../../api";
import {Link, useHistory} from "react-router-dom";
import {Autocomplete, Dialog, DialogActions, DialogContent, FormControl, MenuItem, Select} from "@mui/material";
import useDebounceValue from "../hooks/use-debounce";
import {toast} from "react-toastify";
import TextField from "@material-ui/core/TextField";
import {DataGrid} from "@material-ui/data-grid";
import InfoIcon from "@mui/icons-material/Info";
import QuestionBankDetails from "../questionbank/QuestionBankDetails";
import AddCircleIcon from "@material-ui/icons/AddCircle";
import DeleteIcon from "@material-ui/icons/Delete";
import {parseHTMLToString} from "../ultils/DataUltils";
import {errorNoti} from "../../../../utils/notification";

const baseColumn = {
  sortable: false,
};

const rowsPerPage = [5, 10, 20];

function TestBankAddQuestion(props) {

  const columns = [
    {
      field: "examTagName",
      headerName: "Tag",
      minWidth: 150,
      ...baseColumn,
      renderCell: (rowData) => {
        const result = rowData.row.examTags.map(item => item?.name).join(', ')
        return (
          <div style={{fontStyle: 'italic'}}>{result}</div>
        )
      }
    },
    {
      field: "code",
      headerName: "Mã câu hỏi",
      minWidth: 150,
      ...baseColumn
    },
    {
      field: "examSubjectName",
      headerName: "Môn học",
      minWidth: 150,
      ...baseColumn
    },
    {
      field: "content",
      headerName: "Nội dung câu hỏi",
      ...baseColumn,
      flex: 1,
      renderCell: (rowData) => {
        return parseHTMLToString(rowData.value)
      }
    },
    {
      field: "type",
      headerName: "Loại câu hỏi",
      ...baseColumn,
      minWidth: 130,
      renderCell: (rowData) => {
        if(rowData.value === 0){
          return (
            <strong style={{color: '#716DF2'}}>Trắc nghiệm</strong>
          )
        }else if(rowData.value === 1){
          return (
            <strong style={{color: '#61bd6d'}}>Tự luận</strong>
          )
        }else{
          return 'Tất cả'
        }
      },
    },
    {
      field: "level",
      headerName: "Mức độ",
      ...baseColumn,
      minWidth: 130,
      renderCell: (rowData) => {
        if(rowData.value === "EASY"){
          return (
            <strong style={{color: '#61bd6d'}}>Dễ</strong>
          )
        }else if(rowData.value === "MEDIUM"){
          return (
            <strong style={{color: '#716DF2'}}>Trung bình</strong>
          )
        }else if(rowData.value === "HARD"){
          return (
            <strong style={{color: 'red'}}>Khó</strong>
          )
        }else{
          return ''
        }
      },
    },
    {
      field: "",
      headerName: "",
      sortable: false,
      minWidth: 50,
      maxWidth: 50,
      renderCell: (rowData) => {
        return (
          <Box display="flex" justifyContent="space-between" alignItems='center' width="100%">
            <InfoIcon style={{cursor: 'pointer'}} onClick={(data) => handleDetailsQuestion(rowData?.row)}/>
          </Box>
        )
      }
    },
  ];

  const columnsSelected = [
    {
      field: "examTagName",
      headerName: "Tag",
      minWidth: 150,
      ...baseColumn,
      renderCell: (rowData) => {
        const result = rowData.row.examTags.map(item => item?.name).join(', ')
        return (
          <div style={{fontStyle: 'italic'}}>{result}</div>
        )
      }
    },
    {
      field: "code",
      headerName: "Mã câu hỏi",
      minWidth: 150,
      ...baseColumn
    },
    {
      field: "examSubjectName",
      headerName: "Môn học",
      minWidth: 150,
      ...baseColumn
    },
    {
      field: "content",
      headerName: "Nội dung câu hỏi",
      ...baseColumn,
      flex: 1,
      renderCell: (rowData) => {
        return parseHTMLToString(rowData.value)
      }
    },
    {
      field: "type",
      headerName: "Loại câu hỏi",
      ...baseColumn,
      minWidth: 130,
      renderCell: (rowData) => {
        if(rowData.value === 0){
          return (
            <strong style={{color: '#716DF2'}}>Trắc nghiệm</strong>
          )
        }else if(rowData.value === 1){
          return (
            <strong style={{color: '#61bd6d'}}>Tự luận</strong>
          )
        }else{
          return 'Tất cả'
        }
      },
    },
    {
      field: "level",
      headerName: "Mức độ",
      ...baseColumn,
      minWidth: 130,
      renderCell: (rowData) => {
        if(rowData.value === "EASY"){
          return (
            <strong style={{color: '#61bd6d'}}>Dễ</strong>
          )
        }else if(rowData.value === "MEDIUM"){
          return (
            <strong style={{color: '#716DF2'}}>Trung bình</strong>
          )
        }else if(rowData.value === "HARD"){
          return (
            <strong style={{color: 'red'}}>Khó</strong>
          )
        }else{
          return ''
        }
      },
    },
    {
      field: "",
      headerName: "",
      sortable: false,
      minWidth: 50,
      maxWidth: 50,
      renderCell: (rowData) => {
        return (
          <Box display="flex" justifyContent="space-between" alignItems='center' width="100%">
            <InfoIcon style={{cursor: 'pointer'}} onClick={(data) => handleDetailsQuestion(rowData?.row)}/>
            <DeleteIcon style={{cursor: 'pointer', color: 'red'}} onClick={(data) => handleDeleteQuestionSelected(rowData?.row)}/>
          </Box>
        )
      }
    },
  ];

  const questionTypes = [
    {
      value: 'all',
      name: 'Tất cả'
    },
    {
      value: 0,
      name: 'Trắc nghiệm'
    },
    {
      value: 1,
      name: 'Tự luận'
    }
  ]

  const questionLevels = [
    {
      value: 'all',
      name: 'Tất cả'
    },
    {
      value: "EASY",
      name: 'Dễ'
    },
    {
      value: "MEDIUM",
      name: 'Trung bình'
    },
    {
      value: "HARD",
      name: 'Khó'
    },
  ]

  const { open, setOpen, onSubmit} = props;

  const [questionList, setQuestionList] = useState([])
  const [questionSelectionList, setQuestionSelectionList] = useState([])
  const [questionSelectedList, setQuestionSelectedList] = useState([])
  const [page, setPage] = useState(0)
  const [pageSize, setPageSize] = useState(5)
  const [totalCount, setTotalCount] = useState(0)
  const [keywordFilter, setKeywordFilter] = useState("")
  const [typeFilter, setTypeFilter] = useState('all')
  const [levelFilter, setLevelFilter] = useState('all');
  const [examSubjectIdFilter, setExamSubjectIdFilter] = useState('all');
  const [examTagsFilter, setExamTagsFilter] = useState([]);
  const [examSubjects, setExamSubjects] = useState([]);
  const [questionTags, setQuestionTags] = useState([]);
  const [openDetailsDialog, setOpenDetailsDialog] = useState(false);
  const [questionDetails, setQuestionDetails] = useState(null)

  const debouncedKeywordFilter = useDebounceValue(keywordFilter, 500)

  useEffect(() => {
    getAllQuestionTag()
    getAllExamSubject()
  }, []);

  useEffect(() => {
    filterQuestion()
  }, [page, pageSize, debouncedKeywordFilter, typeFilter, levelFilter, examSubjectIdFilter, examTagsFilter]);

  const filterQuestion = () =>{
    const queryParams = new URLSearchParams({
      page: page,
      size: pageSize,
      keyword: keywordFilter,
    })
    if (typeFilter != null && typeFilter !== "all") queryParams.append('type', typeFilter)
    if (levelFilter != null && levelFilter !== "all") queryParams.append('level', levelFilter)
    if (examSubjectIdFilter != null && examSubjectIdFilter !== "all") queryParams.append('examSubjectId', examSubjectIdFilter)
    if(examTagsFilter.length > 0){
      const ids = examTagsFilter.map(item => item?.id).join(',')
      queryParams.append('examTagIds', ids)
    }
    request(
      "get",
      `/exam-question/filter?${queryParams}`,
      (res) => {
        if(res.status === 200){
          setQuestionList(res.data.content);
          setTotalCount(res.data.totalElements);
        }else {
          errorNoti(res)
        }
      },
      { onError: (e) => errorNoti(e) },
    );
  }

  const getAllQuestionTag = () => {
    request(
      "get",
      `/exam-tag/get-all`,
      (res) => {
        if(res.status === 200){
          setQuestionTags(res.data)
        }
      },
      { onError: (e) => toast.error(e) }
    );
  }

  const getAllExamSubject = () => {
    request(
      "get",
      `/exam-subject/get-all`,
      (res) => {
        if(res.status === 200){
          let tmpData = res.data
          tmpData.unshift({
            id: 'all',
            name: 'Tất cả'
          })
          setExamSubjects(tmpData)
        }
      },
      { onError: (e) => toast.error(e) }
    );
  }

  const detailsQuestion = (id) =>{
    const queryParams = new URLSearchParams({
      id: id
    })
    request(
      "get",
      `/exam-question/details?${queryParams}`,
      (res) => {
        if(res.data.resultCode === 200){
          setQuestionDetails(res.data.data)
          setOpenDetailsDialog(true)
        }else{
          toast.error(res.data.resultMsg)
        }
      },
      { onError: (e) => toast.error(e) },
    );
  }

  const handleDetailsQuestion = (rowData) => {
    detailsQuestion(rowData.id)
  };

  const closeDialog = () => {
    setQuestionSelectionList([])
    setQuestionSelectedList([])
    setOpen(false)
  }

  const onClickAddToSelectedList = () => {
    const selectedRowsData = questionSelectionList.map((id) => questionList.find((row) => row.id === id));
    setQuestionSelectedList(questionSelectedList.concat(selectedRowsData))
    setQuestionSelectionList([])
  }

  const handleDeleteQuestionSelected = (data) => {
    let tmpQuestionSelectedList = questionSelectedList.filter(item => item.id !== data.id);
    setQuestionSelectedList(tmpQuestionSelectedList)
  }

  const handleAdd = () => {
    onSubmit(questionSelectedList)
    closeDialog()
  }

  return (
    <div>
      <Dialog open={open} fullWidth maxWidth="lg">
        <DialogTitle>Thêm câu hỏi vào đề thi</DialogTitle>
        <DialogContent>
          <Card elevation={5}>
            <CardHeader
              title={
                <Box display="flex" justifyContent="space-between" alignItems="end" width="100%">
                  <Box display="flex" flexDirection="column" width="80%">
                    <h5 style={{marginTop: '0', paddingTop: '0'}}>Tìm kiếm trong Ngân hàng câu hỏi</h5>
                    <Box display="flex" justifyContent="flex-start" width="100%">
                      <TextField
                        autoFocus
                        id="questionCode"
                        label="Nội dung tìm kiếm"
                        placeholder="Tìm kiếm theo code hoặc nội dung"
                        value={keywordFilter}
                        style={{width: "300px", marginRight: "16px"}}
                        onChange={(event) => {
                          setKeywordFilter(event.target.value);
                        }}
                        InputLabelProps={{
                          shrink: true,
                        }}
                      />

                      <TextField
                        id="questionType"
                        select
                        label="Loại câu hỏi"
                        style={{width: "150px", marginRight: "16px"}}
                        value={typeFilter}
                        onChange={(event) => {
                          setTypeFilter(event.target.value);
                        }}
                      >
                        {
                          questionTypes.map(item => {
                            return (
                              <MenuItem value={item.value}>{item.name}</MenuItem>
                            )
                          })
                        }
                      </TextField>

                      <TextField
                        required
                        autoFocus
                        id="questionLevel"
                        select
                        label="Mức độ"
                        style={{ width: "150px", marginRight: "16px"}}
                        value={levelFilter}
                        onChange={(event) => {
                          setLevelFilter(event.target.value);
                        }}
                      >
                        {
                          questionLevels.map(item => {
                            return (
                              <MenuItem value={item.value}>{item.name}</MenuItem>
                            )
                          })
                        }
                      </TextField>

                      <TextField
                        required
                        autoFocus
                        id="examSubjectId"
                        select
                        label="Môn học"
                        style={{ width: "150px", marginRight: "16px"}}
                        value={examSubjectIdFilter}
                        onChange={(event) => {
                          setExamSubjectIdFilter(event.target.value);
                        }}
                      >
                        {
                          examSubjects.map(item => {
                            return (
                              <MenuItem value={item.id}>{item.name}</MenuItem>
                            )
                          })
                        }
                      </TextField>
                    </Box>
                    <Box display="flex" justifyContent="flex-start" width="100%">
                      <Autocomplete
                        multiple
                        id="examTagIds"
                        options={questionTags}
                        getOptionLabel={(item) => item?.name}
                        value={examTagsFilter}
                        onChange={(event, newValue) => {
                          setExamTagsFilter(newValue);
                        }}
                        renderInput={(params) => (
                          <TextField
                            {...params}
                            style={{width: "300px", marginRight: "16px"}}
                            variant="standard"
                            label="Tag"
                          />
                        )}
                      />
                    </Box>
                  </Box>
                  <Box display="flex" justifyContent="flex-end" width="20%">
                    <Button
                      variant="contained"
                      disabled={questionSelectionList.length < 1}
                      color="primary"
                      onClick={onClickAddToSelectedList}
                      startIcon={<AddCircleIcon />}
                    >
                      Thêm vào danh sách
                    </Button>
                  </Box>
                </Box>
              }/>
            <CardContent>
              <DataGrid
                rowCount={totalCount}
                rows={questionList}
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
                checkboxSelection
                isRowSelectable={(params) => !questionSelectedList.includes(params.row)}
                onSelectionModelChange = {(ids) => setQuestionSelectionList(ids)}
                selectionModel={questionSelectionList}
              />
            </CardContent>
          </Card>

          <Card elevation={5} >
            <CardHeader title={
              <h5 style={{marginTop: '0', paddingTop: '0'}}>Danh sách câu hỏi đã chọn</h5>
            }/>
            <CardContent>
              <DataGrid
                rows={questionSelectedList}
                columns={columnsSelected}
                disableColumnMenu
                autoHeight
              />
            </CardContent>
          </Card>
        </DialogContent>
        <DialogActions>
          <div>
            <Button
              variant="contained"
              onClick={closeDialog}
            >
              Hủy
            </Button>
            <Button
              variant="contained"
              color="primary"
              disabled={questionSelectedList.length < 1}
              style={{marginLeft: "15px"}}
              onClick={handleAdd}
            >
              Lưu
            </Button>
          </div>
        </DialogActions>
      </Dialog>

      {
        openDetailsDialog && (
          <QuestionBankDetails
            open={openDetailsDialog}
            setOpen={setOpenDetailsDialog}
            question={questionDetails}
          />
        )
      }
    </div>
  );
}

const screenName = "MENU_EXAM_QUESTION_BANK";
// export default withScreenSecurity(QuestionBank, screenName, true);
export default TestBankAddQuestion;
