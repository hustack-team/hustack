import React, {useEffect, useState} from 'react';
import withScreenSecurity from "../../../withScreenSecurity";
import {Box, Button, Card, CardContent, CardHeader, Input} from "@material-ui/core";
import AddCircleIcon from "@material-ui/icons/AddCircle";
import {request} from "../../../../api";
import {Link, useHistory} from "react-router-dom";
import {Autocomplete, FormControl, MenuItem, Select} from "@mui/material";
import useDebounceValue from "../hooks/use-debounce";
import {toast} from "react-toastify";
import TextField from "@material-ui/core/TextField";
import QuestionBankDelete from "./QuestionBankDelete";
import QuestionBankDetails from "./QuestionBankDetails";
import {DataGrid} from "@material-ui/data-grid";
import InfoIcon from "@mui/icons-material/Info";
import EditIcon from "@material-ui/icons/Edit";
import DeleteIcon from "@material-ui/icons/Delete";
import {parseHTMLToString, parseToString} from "../ultils/DataUltils";

const baseColumn = {
  sortable: false,
};

const rowsPerPage = [5, 10, 20];

function QuestionBank(props) {

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
      minWidth: 170,
      renderCell: (rowData) => {
        return parseHTMLToString(rowData.value)
      }
    },
    // {
    //   field: "answer",
    //   headerName: "Đáp án",
    //   ...baseColumn,
    //   flex: 1,
    //   minWidth: 170,
    //   renderCell: (rowData) => {
    //     return parseHTMLToString(rowData.value)
    //   }
    // },
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
      minWidth: 120,
      maxWidth: 120,
      renderCell: (rowData) => {
        return (
          <Box display="flex" justifyContent="space-between" alignItems='center' width="100%">
            <InfoIcon style={{cursor: 'pointer'}} onClick={(data) => handleDetailsQuestion(rowData?.row)}/>
            <EditIcon style={{cursor: 'pointer'}} onClick={(data) => handleUpdateQuestion(rowData?.row)}/>
            <DeleteIcon style={{cursor: 'pointer', color: 'red'}} onClick={(data) => handleDeleteQuestion(rowData?.row)}/>
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

  const [questionList, setQuestionList] = useState([])
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
  const [openDeleteDialog, setOpenDeleteDialog] = useState(false);
  const [idDelete, setIdDelete] = useState("")
  const [openDetailsDialog, setOpenDetailsDialog] = useState(false);
  const [questionDetails, setQuestionDetails] = useState(null)

  const debouncedKeywordFilter = useDebounceValue(keywordFilter, 500)
  const history = useHistory();

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
      examTags: examTagsFilter,
    })
    if (typeFilter != null && typeFilter !== "all") queryParams.append('type', typeFilter)
    if (levelFilter != null && levelFilter !== "all") queryParams.append('level', levelFilter)
    if (examSubjectIdFilter != null && examSubjectIdFilter !== "all") queryParams.append('examSubjectId', examSubjectIdFilter)
    request(
      "get",
      `/exam-question/filter?${queryParams}`,
      (res) => {
        if(res.status === 200){
          setQuestionList(res.data.content);
          setTotalCount(res.data.totalElements);
        }else {
          toast.error(res)
        }
      },
      { onError: (e) => toast.error(e) },
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

  const onClickCreateNewButton = () => {
    history.push({
      pathname: "/exam/create-update-question-bank",
      state: {
        question: {
          code: "",
          type: 1,
          level: 'EASY',
          examSubjectId: "",
          examTags: [],
          content: "",
          filePath: "",
          numberAnswer: "",
          contentAnswer1: "",
          contentAnswer2: "",
          contentAnswer3: "",
          contentAnswer4: "",
          contentAnswer5: "",
          multichoice: false,
          answer: "",
          explain: ""
        },
        isCreate: true
      },
    });
  };

  const handleUpdateQuestion = (rowData) => {
    history.push({
      pathname: "/exam/create-update-question-bank",
      state: {
        question: {
          code: rowData.code,
          type: rowData.type,
          level: rowData.level,
          examSubjectId: rowData.examSubjectId,
          examTags: rowData.examTags,
          content: parseToString(rowData.content),
          filePath: rowData.filePath,
          numberAnswer: rowData.numberAnswer,
          contentAnswer1: parseToString(rowData.contentAnswer1),
          contentAnswer2: parseToString(rowData.contentAnswer2),
          contentAnswer3: parseToString(rowData.contentAnswer3),
          contentAnswer4: parseToString(rowData.contentAnswer4),
          contentAnswer5: parseToString(rowData.contentAnswer5),
          multichoice: rowData.multichoice,
          answer: parseToString(rowData.answer),
          explain: parseToString(rowData.explain)
        },
        isCreate: false
      },
    });
  };

  const handleDetailsQuestion = (rowData) => {
    detailsQuestion(rowData.id)
  };

  const handleDeleteQuestion = (rowData) => {
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
                <h4 style={{marginTop: 0, paddingTop: 0}}>Ngân hàng câu hỏi</h4>
                <Box display="flex" justifyContent="flex-start" width="100%">
                  <TextField
                    autoFocus
                    id="questionCode"
                    label="Nội dung tìm kiếm"
                    placeholder="Tìm kiếm theo code hoặc nội dung"
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
                    id="questionType"
                    select
                    label="Loại câu hỏi"
                    style={{ width: "150px", marginRight: "16px"}}
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
                  color="primary"
                  onClick={onClickCreateNewButton}
                  startIcon={<AddCircleIcon />}
                  style={{ marginRight: 16 }}
                >
                  Thêm mới
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
          />
        </CardContent>
      </Card>
      {
        openDetailsDialog && (
          <QuestionBankDetails
            open={openDetailsDialog}
            setOpen={setOpenDetailsDialog}
            question={questionDetails}
          />
        )
      }
      <QuestionBankDelete
        open={openDeleteDialog}
        setOpen={setOpenDeleteDialog}
        id={idDelete}
        onReloadQuestions={() => {
          filterQuestion()
        }}
      />
    </div>
  );
}

const screenName = "MENU_EXAM_QUESTION_BANK";
export default withScreenSecurity(QuestionBank, screenName, true);
// export default QuestionBank;
