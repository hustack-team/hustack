import React, {useEffect, useState} from 'react';
import {
  Box, 
  Autocomplete, 
  Card, 
  CardContent, 
  CircularProgress, 
  MenuItem, 
  Typography, 
  TextField, 
  CardActions
} from "@mui/material";
import {request} from "../../../../api";
import { LocalizationProvider } from "@mui/x-date-pickers";
import {AdapterDateFns} from "@mui/x-date-pickers/AdapterDateFns";
import {toast} from "react-toastify";
import RichTextEditor from "../../../common/editor/RichTextEditor";
import {DropzoneArea} from "material-ui-dropzone";
import {makeStyles} from "@material-ui/core/styles";
import {useHistory} from "react-router-dom";
import {useLocation} from "react-router";
import {getFilenameFromString, getFilePathFromString} from "../ultils/FileUltils";
import {AttachFileOutlined, Delete, Style} from "@mui/icons-material";
import QuestionFilePreview from "./QuestionFilePreview";
import withScreenSecurity from "../../../withScreenSecurity";
import QuestionTagManagement from "./QuestionTagManagement";
import PrimaryButton from "../../../button/PrimaryButton";
import TertiaryButton from "../../../button/TertiaryButton";

const useStyles = makeStyles((theme) => ({
  root: {
    "& .MuiTextField-root": {
      margin: theme.spacing(1),
      width: 200,
    },
  },
  formControl: {
    margin: theme.spacing(1),
    minWidth: 120,
    maxWidth: 300,
  },
}));
const ITEM_HEIGHT = 48;
const ITEM_PADDING_TOP = 8;
const MenuProps = {
  PaperProps: {
    style: {
      maxHeight: ITEM_HEIGHT * 4.5 + ITEM_PADDING_TOP,
      width: 250,
    },
  },
};

function QuestionBankCreateUpdate(props) {

  const questionTypes = [
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

  const numberAnswers = [
    {
      value: 2,
      name: '2 đán án'
    },
    {
      value: 3,
      name: '3 đán án'
    },
    {
      value: 4,
      name: '4 đán án'
    },
    {
      value: 5,
      name: '5 đán án'
    },
  ]

  const multichoices = [
    {
      value: false,
      name: '1 đáp án'
    },
    {
      value: true,
      name: 'Nhiều đáp án'
    }
  ]

  const classes = useStyles();
  const history = useHistory();
  const location = useLocation();
  const question = location.state?.question
  const isCreate = location.state?.isCreate

  if(isCreate === undefined){
    window.location.href = '/exam/question-bank';
  }

  const [code, setCode] = useState(question?.code);
  const [type, setType] = useState(question?.type);
  const [level, setLevel] = useState(question?.level);
  const [examSubjectId, setExamSubjectId] = useState(question?.examSubjectId);
  const [examTags, setExamTags] = useState(question?.examTags);
  const [content, setContent] = useState(question?.content);
  const [filePath, setFilePath] = useState(question?.filePath);
  const [deletePaths, setDeletePaths] = useState([]);
  const [contentFiles, setContentFiles] = useState([]);
  const [numberAnswer, setNumberAnswer] = useState(question?.numberAnswer);
  const [contentAnswer1, setContentAnswer1] = useState(question?.contentAnswer1);
  const [contentAnswer2, setContentAnswer2] = useState(question?.contentAnswer2);
  const [contentAnswer3, setContentAnswer3] = useState(question?.contentAnswer3);
  const [contentAnswer4, setContentAnswer4] = useState(question?.contentAnswer4);
  const [contentAnswer5, setContentAnswer5] = useState(question?.contentAnswer5);
  const [multichoice, setMultichoice] = useState(question?.multichoice);
  const [answer, setAnswer] = useState(question?.answer);
  const [explain, setExplain] = useState(question?.explain);
  const [isLoading, setIsLoading] = useState(false);
  const [openFilePreviewDialog, setOpenFilePreviewDialog] = useState(false);
  const [filePreview, setFilePreview] = useState(null);
  const [examSubjects, setExamSubjects] = useState([]);
  const [questionTags, setQuestionTags] = useState([]);
  const [openQuestionTagManagementDialog, setOpenQuestionTagManagementDialog] = useState(false);

  useEffect(() => {
    getAllQuestionTag()
    getAllExamSubject()
  }, []);

  useEffect(() => {
    if(!openQuestionTagManagementDialog){
      getAllQuestionTag()
    }
  }, [openQuestionTagManagementDialog]);

  useEffect(() => {
    console.log('examTags',examTags)
  }, [examTags]);

  const saveQuestion = () =>{
    const body = {
      code: code,
      type:  type,
      level: level,
      examSubjectId: examSubjectId,
      examTags: examTags,
      content:  content,
      filePath: filePath,
      deletePaths: isCreate ? null : deletePaths,
      numberAnswer: type === 0 ? numberAnswer : null,
      contentAnswer1: type === 0 ? contentAnswer1 : null,
      contentAnswer2: type === 0 ? contentAnswer2 : null,
      contentAnswer3: type === 0 ? contentAnswer3 : null,
      contentAnswer4: type === 0 ? contentAnswer4 : null,
      contentAnswer5: type === 0 ? contentAnswer5 : null,
      multichoice: type === 0 ? multichoice : null,
      answer:  answer,
      explain:  explain
    }
    if(!validateBody(body)){
      return
    }

    let formData = new FormData();
    formData.append("body", new Blob([JSON.stringify(body)], {type: 'application/json'}));
    for (const file of contentFiles) {
      formData.append("files", file);
    }

    const config = {
      headers: {
        "content-type": "multipart/form-data",
      },
    };

    setIsLoading(true)
    request(
      isCreate ? "post" : "put",
      "/exam-question",
      (res) => {
        if(res.status === 200){
          if(res.data.resultCode === 200){
            toast.success(res.data.resultMsg)
            setIsLoading(false)
            history.push("/exam/question-bank")
          }else{
            toast.error(res.data.resultMsg)
            setIsLoading(false)
          }
        }else {
          toast.error(res)
          setIsLoading(false)
        }
      },
      { onError: (e) => toast.error(e) },
      formData,
      config,
    );
  }

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

  const getAllExamSubject = () => {
    request(
      "get",
      `/exam-subject/all`,
      (res) => {
        if(res.status === 200){
          setExamSubjects(res.data)
        }
      },
      { onError: (e) => toast.error(e) }
    );
  }

  const validateBody = (body) => {
    if(body.code == null || body.code === ''){
      toast.error('Mã câu hỏi không được bỏ trống')
      return false
    }
    if(body.content == null || body.content === ''){
      toast.error('Nội dung câu hỏi không được bỏ trống')
      return false
    }
    if(body.examSubjectId == null || body.examSubjectId === ''){
      toast.error('Lựa chọn môn học')
      return false
    }
    if(body.type === 0){
      if(body.contentAnswer1 == null || body.contentAnswer1 === ''){
        toast.error('Nội dung phương án 1 không được bỏ trống')
        return false
      }
      if((body.contentAnswer2 == null || body.contentAnswer2 === '') && numberAnswer >= 2){
        toast.error('Nội dung phương án 2 không được bỏ trống')
        return false
      }
      if((body.contentAnswer3 == null || body.contentAnswer3 === '') && numberAnswer >= 3){
        toast.error('Nội dung phương án 3 không được bỏ trống')
        return false
      }
      if((body.contentAnswer4 == null || body.contentAnswer4 === '') && numberAnswer >= 4){
        toast.error('Nội dung phương án 4 không được bỏ trống')
        return false
      }
      if((body.contentAnswer5 == null || body.contentAnswer5 === '') && numberAnswer >= 5){
        toast.error('Nội dung phương án 5 không được bỏ trống')
        return false
      }
    }
    if(body.answer == null || body.answer === ''){
      toast.error('Đáp án không được bỏ trống')
      return false
    }
    return true
  }

  const handleKeyPress = (event) => {
    const regex = /^[a-zA-Z0-9]+$/
    if (!regex.test(event.key)) {
      event.preventDefault();
    }
  }

  const handleOpenFilePreviewDialog = (data) => {
    setOpenFilePreviewDialog(true)
    setFilePreview(getFilePathFromString(data))
  };

  const handleDeleteFile = (data) => {
    let tmpFilePath = []
    filePath.split(';').map(item => {
      if(item !== data){
        tmpFilePath.push(item)
      }
    })
    setFilePath(tmpFilePath.join(';'))

    let tmpDeletePaths = deletePaths
    tmpDeletePaths.push(data)
    setDeletePaths(tmpDeletePaths)
  }

  return (
    <div>
      <LocalizationProvider dateAdapter={AdapterDateFns}>
        <Card>
          <CardContent>
            <Typography variant="h5" component="h2">
              {
                isCreate ? (<h4  style={{margin: 0, padding: 0}}>Thêm mới câu hỏi</h4>) : (<h4  style={{margin: 0, padding: 0}}>Cập nhật câu hỏi</h4>)
              }
            </Typography>
            <form className={classes.root} noValidate autoComplete="off">
              <div>
                <div>
                  <TextField
                    autoFocus
                    required
                    onKeyPress={handleKeyPress}
                    disabled={!isCreate}
                    id="questionCode"
                    label="Mã câu hỏi"
                    placeholder="Nhập mã câu hỏi"
                    size="small"
                    value={code}
                    onChange={(event) => {
                      setCode(event.target.value);
                    }}
                    InputLabelProps={{
                      shrink: true,
                    }}
                  />

                  <TextField
                    required
                    autoFocus
                    id="questionType"
                    select
                    label="Loại câu hỏi"
                    size="small"
                    value={type}
                    onChange={(event) => {
                      setType(event.target.value);
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

                  {
                    (type === 0) && (
                      <TextField
                        required
                        id="numberAnswer"
                        select
                        label="Số đáp án"
                        size="small"
                        value={numberAnswer}
                        onChange={(event) => {
                          setNumberAnswer(event.target.value);
                        }}
                      >
                        {
                          numberAnswers.map(item => {
                            return (
                              <MenuItem value={item.value}>{item.name}</MenuItem>
                            )
                          })
                        }
                      </TextField>
                    )
                  }

                  {
                    (type === 0) && (
                      <TextField
                        required
                        id="multichoice"
                        select
                        label="Số đáp án đúng"
                        size="small"
                        value={multichoice}
                        onChange={(event) => {
                          setMultichoice(event.target.value);
                        }}
                      >
                        {
                          multichoices.map(item => {
                            return (
                              <MenuItem value={item.value}>{item.name}</MenuItem>
                            )
                          })
                        }
                      </TextField>
                    )
                  }
                </div>

                <div style={{display: "flex"}}>
                  <TextField
                    required
                    autoFocus
                    id="questionLevel"
                    select
                    label="Mức độ"
                    size="small"
                    value={level}
                    onChange={(event) => {
                      setLevel(event.target.value);
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
                    size="small"
                    value={examSubjectId}
                    onChange={(event) => {
                      setExamSubjectId(event.target.value);
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
                </div>

                <div style={{display: "flex"}}>
                  <Autocomplete
                    multiple
                    id="examTagIds"
                    options={questionTags}
                    getOptionLabel={(item) => item?.name}
                    value={examTags}
                    onChange={(event, newValue) => {
                      setExamTags(newValue);
                    }}
                    renderInput={(params) => (
                      <TextField
                        {...params}
                        style={{width: "420px"}}
                        variant="standard"
                        label="Tag"
                        size="small"
                      />
                    )}
                  />

                  <PrimaryButton
                    variant="outlined"
                    color="primary"
                    style={{height: "55px"}}
                    startIcon={<Style/>}
                    onClick={() => setOpenQuestionTagManagementDialog(true)}
                  >
                    Manage Tags
                  </PrimaryButton>
                </div>

                <div>
                  <Typography variant="h6">Nội dung câu hỏi</Typography>
                  <RichTextEditor
                    content={content}
                    onContentChange={(content) =>
                      setContent(content)
                    }
                  />
                  {/*<FileUploader*/}
                  {/*  onChange={(files) => setContentFiles(files)}*/}
                  {/*  multiple*/}
                  {/*/>*/}
                  {
                    (filePath) && (filePath.split(';').map(item => {
                        return (
                          <div style={{display: 'flex', alignItems : 'center'}}>
                            <AttachFileOutlined></AttachFileOutlined>
                            <p style={{fontWeight : 'bold', cursor : 'pointer'}} onClick={() => handleOpenFilePreviewDialog(item)}>{getFilenameFromString(item)}</p>
                            <Delete style={{color: 'red', cursor: 'pointer', marginLeft: '10px'}} onClick={() => handleDeleteFile(item)}/>
                          </div>
                        )
                      })
                    )
                  }
                </div>

                <div>
                  <Typography
                    variant="subtitle1"
                    display="block"
                    style={{margin: "5px 0 0 7px", width: "100%"}}
                  >
                    File đính kèm
                  </Typography>
                  <DropzoneArea
                    dropzoneClass={classes.dropZone}
                    filesLimit={20}
                    maxFileSize={5000000}
                    showPreviews={true}
                    showPreviewsInDropzone={false}
                    useChipsForPreview
                    dropzoneText="Kéo và thả tệp vào đây hoặc nhấn để chọn tệp"
                    previewText="Xem trước:"
                    previewChipProps={{
                      variant: "outlined",
                      color: "primary",
                      size: "medium",
                    }}
                    getFileAddedMessage={(fileName) =>
                      `Tệp ${fileName} tải lên thành công`
                    }
                    getFileRemovedMessage={(fileName) => `Tệp ${fileName} đã loại bỏ`}
                    getFileLimitExceedMessage={(filesLimit) =>
                      `Vượt quá số lượng tệp tối đa được cho phép. Chỉ được phép tải lên tối đa ${filesLimit} tệp.`
                    }
                    alertSnackbarProps={{
                      anchorOrigin: {vertical: "bottom", horizontal: "right"},
                      autoHideDuration: 1800,
                    }}
                    onChange={(files) => setContentFiles(files)}
                  ></DropzoneArea>
                </div>

                {
                  (type === 0) && (
                    <div>
                      <Typography variant="h6">Nội dung phương án 1</Typography>
                      <RichTextEditor
                        content={contentAnswer1}
                        onContentChange={(contentAnswer1) =>
                          setContentAnswer1(contentAnswer1)
                        }
                      />
                    </div>
                  )
                }
                {
                  (type === 0 && numberAnswer >= 2) && (
                    <div>
                      <Typography variant="h6">Nội dung phương án 2</Typography>
                      <RichTextEditor
                        content={contentAnswer2}
                        onContentChange={(contentAnswer2) =>
                          setContentAnswer2(contentAnswer2)
                        }
                      />
                    </div>
                  )
                }
                {
                  (type === 0 && numberAnswer >= 3) && (
                    <div>
                      <Typography variant="h6">Nội dung phương án 3</Typography>
                      <RichTextEditor
                        content={contentAnswer3}
                        onContentChange={(contentAnswer3) =>
                          setContentAnswer3(contentAnswer3)
                        }
                      />
                    </div>
                  )
                }
                {
                  (type === 0 && numberAnswer >= 4) && (
                    <div>
                      <Typography variant="h6">Nội dung phương án 4</Typography>
                      <RichTextEditor
                        content={contentAnswer4}
                        onContentChange={(contentAnswer4) =>
                          setContentAnswer4(contentAnswer4)
                        }
                      />
                    </div>
                  )
                }
                {
                  (type === 0 && numberAnswer >= 5) && (
                    <div>
                      <Typography variant="h6">Nội dung phương án 5</Typography>
                      <RichTextEditor
                        content={contentAnswer5}
                        onContentChange={(contentAnswer5) =>
                          setContentAnswer5(contentAnswer5)
                        }
                      />
                    </div>
                  )
                }

                <Box display="flex" width="100%">
                  {
                    type === 0 && (
                      <div>
                        <Typography variant="h6">Nội dung đáp án</Typography>
                        <TextField
                          autoFocus
                          label="Nếu nhiều lựa chọn, các đáp án cách nhau dấu ,"
                          placeholder="Ví dụ: 2,3"
                          value={answer}
                          style={{width: '350px'}}
                          size="small"
                          onChange={(event) => {
                            setAnswer(event.target.value);
                          }}
                          InputLabelProps={{
                            shrink: true,
                          }}
                        />
                      </div>
                    )
                  }
                  {
                    type === 1 && (
                      <div>
                        <Typography variant="h6">Nội dung đáp án *</Typography>
                        <RichTextEditor
                          content={answer}
                          onContentChange={(answer) =>
                            setAnswer(answer)
                          }
                        />
                      </div>
                    )
                  }

                  <div>
                    <Typography variant="h6">Nội dung giải thích</Typography>
                    <RichTextEditor
                      content={explain}
                      onContentChange={(explain) =>
                        setExplain(explain)
                      }
                    />
                  </div>
                </Box>
              </div>
            </form>
          </CardContent>
          <CardActions style={{justifyContent: 'flex-end'}}>
            <TertiaryButton
              variant="outlined"
              onClick={() => history.push("/exam/question-bank")}
            >
              Hủy
            </TertiaryButton>
            <PrimaryButton
              disabled={isLoading}
              variant="contained"
              color="primary"
              style={{marginLeft: "15px"}}
              onClick={saveQuestion}
              type="submit"
            >
              {isLoading ? <CircularProgress/> : "Lưu"}
            </PrimaryButton>
          </CardActions>
          <QuestionFilePreview
            open={openFilePreviewDialog}
            setOpen={setOpenFilePreviewDialog}
            file={filePreview}>
          </QuestionFilePreview>
          <QuestionTagManagement
            open={openQuestionTagManagementDialog}
            setOpen={setOpenQuestionTagManagementDialog}
            data={questionTags}
          ></QuestionTagManagement>
        </Card>
      </LocalizationProvider>
    </div>
  );
}

const screenName = "MENU_EXAM_QUESTION_BANK";
export default withScreenSecurity(QuestionBankCreateUpdate, screenName, true);
// export default QuestionBankCreateUpdate;
