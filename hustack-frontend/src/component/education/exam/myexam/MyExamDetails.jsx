import React, {useEffect, useState} from 'react';
import {
  Box,
  Card,
  CardContent,
  CircularProgress,
  TextField,
  CardActions,
  Checkbox,
  FormControlLabel,
  Radio,
  RadioGroup,
  FormGroup
} from "@material-ui/core";
import {request} from "../../../../api";
import { LocalizationProvider } from "@mui/x-date-pickers";
import {AdapterDateFns} from "@mui/x-date-pickers/AdapterDateFns";
import {toast} from "react-toastify";
import RichTextEditor from "../../../common/editor/RichTextEditor";
import {makeStyles} from "@material-ui/core/styles";
import {useHistory} from "react-router-dom";
import {useLocation} from "react-router";
import {formatDateTime, formatTimeToMMSS} from "../ultils/DateUltils";
import {DropzoneArea} from "material-ui-dropzone";
import {AccessTime, AttachFileOutlined, Cancel, Comment, Timer, CheckCircle, Check} from "@mui/icons-material";
import {
  compressImage,
  getFileCommentFromFileAnswerAndExamResultDetailsId,
  getFilenameFromString,
  getFilePathFromString
} from "../ultils/FileUltils";
import QuestionFilePreview from "../questionbank/QuestionFilePreview";
import {parseHTMLToString} from "../ultils/DataUltils";
import PrimaryButton from "../../../button/PrimaryButton";
import TertiaryButton from "../../../button/TertiaryButton";
import MyExamMonitor from "./MyExamMonitor";
import {useMenu} from "../../../../layout/sidebar/context/MenuContext";

const useStyles = makeStyles((theme) => ({
  root: {
    "& .MuiTextField-root": {
      margin: theme.spacing(1),
      width: 200
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

function MyExamDetails(props) {

  const classes = useStyles();
  const history = useHistory();
  const location = useLocation();
  const data = location.state?.data
  const { openMenu } = useMenu();
  const initialSeconds = data?.examTestDuration * 60 || 0;

  if(data === undefined){
    window.location.href = '/exam/my-exam';
  }

  const [isLoading, setIsLoading] = useState(false);
  const [dataAnswers, setDataAnswers] = useState([]);
  const [tmpTextAnswer, setTmpTextAnswer] = useState("");
  const [answersFiles, setAnswersFiles] = useState([]);
  const [openFilePreviewDialog, setOpenFilePreviewDialog] = useState(false);
  const [filePreview, setFilePreview] = useState(null);
  const [startLoadTime, setStartLoadTime] = useState(null);
  const [startDoing, setStartDoing] = useState((data?.examMonitor && data?.examMonitor > 0) ? true : false);
  const [countdown, setCountdown] = useState(data?.submitedAt ? 0 : initialSeconds);

  useEffect(() => {
    let tmpDataAnswers = []
    let tmpFileAnswers = []
    for(let item of data?.questionList){
      tmpDataAnswers.push({
        questionOrder: item?.questionOrder,
        examQuestionId: item?.questionId,
        answer: ""
      })
      tmpFileAnswers.push({
        questionOrder: item?.questionOrder,
        files: null
      })
    }
    setDataAnswers(tmpDataAnswers)
    setAnswersFiles(tmpFileAnswers)
    setStartLoadTime(new Date());
  }, []);

  useEffect(() => {
    if (countdown > 0) {
      const timer = setInterval(() => {
        setCountdown((prev) => {
          if (prev <= 1) {
            clearInterval(timer);
            // Thực hiện nộp bài
            handleSubmit()
            return 0;
          }
          return prev - 1;
        });
      }, 1000);

      return () => clearInterval(timer);
    }
  }, [countdown]);
  const progress = (countdown / initialSeconds) * 100;
  const strokeDasharray = 283;
  const strokeDashoffset = (progress / 100) * strokeDasharray;

  const handleAnswerCheckboxChange = (questionOrder, answer, isChecked) => {
    if(isChecked){
      if(dataAnswers[questionOrder-1]?.answer === ''){
        dataAnswers[questionOrder-1].answer = answer
      }else{
        dataAnswers[questionOrder-1].answer += ',' + answer
      }
    }else{
      const answersArray = dataAnswers[questionOrder-1].answer.split(',');
      const filteredAnswers = answersArray.filter(item => item !== answer);
      dataAnswers[questionOrder-1].answer = filteredAnswers.join(',');
    }

    setDataAnswers(dataAnswers)
  };

  const handleAnswerRadioChange = (event, questionOrder) => {
    setDataAnswers((prev) => {
      const newAnswers = [...prev];
      newAnswers[questionOrder-1].answer = event.target.value
      return newAnswers;
    })
  };

  const handleAnswerTextChange = (value, questionOrder) => {
    dataAnswers[questionOrder-1].answer = value

    setDataAnswers(dataAnswers)
  };

  const handleAnswerFileChange = async (files, questionOrder) => {
    let tmpFiles = []
    for(let file of files){
      try {
        if (file.type.startsWith('image/')){
          tmpFiles.push(await compressImage(file, 1920, 1920, 1))
        }else{
          tmpFiles.push(file)
        }
      } catch (error) {
        console.error('Error compressing file:', error);
        toast.error('Lỗi khi tải ảnh lên');
      }
    }
    answersFiles[questionOrder-1].files = tmpFiles

    setAnswersFiles(answersFiles)
  }

  const handleSubmit = () => {
    const endLoadTime = new Date();
    const totalTime = Math.round((endLoadTime - startLoadTime) / 60000);

    const body = {
      id: data?.examResultId,
      examStudentTestId: data?.examStudentTestId,
      totalTime: totalTime,
      examResultDetails: dataAnswers
    }

    let tmpAnswersFiles = []
    for(let item of answersFiles){
      if(item?.files != null){
        for(let file of item?.files){
          const fileNameParts = file.name.split('.');
          const newFileName = `${fileNameParts[0]}_${item?.questionOrder}.${fileNameParts[1]}`;

          const updatedFile = new File([file], newFileName, {
            type: file.type,
            lastModified: file.lastModified,
          });

          tmpAnswersFiles.push(updatedFile)
        }
      }
    }

    let formData = new FormData();
    formData.append("body", new Blob([JSON.stringify(body)], {type: 'application/json'}));
    for (const file of tmpAnswersFiles) {
      formData.append("files", file);
    }

    const config = {
      headers: {
        "content-type": "multipart/form-data",
      },
    };

    setIsLoading(true)
    request(
      "post",
      '/exam/student/submissions',
      (res) => {
        if(res.status === 200){
          if(res.data.resultCode === 200){
            toast.success(res.data.resultMsg)
            setIsLoading(false)
            history.push("/exam/my-exam")
          }else{
            toast.error(res.data.resultMsg)
            setIsLoading(false)
          }
          openMenu()
        }else {
          toast.error(res)
          setIsLoading(false)
          openMenu()
        }
      },
      { onError: (e) => toast.error(e) },
      formData,
      config,
    );
  }

  const handleOpenFilePreviewDialog = (data) => {
    setOpenFilePreviewDialog(true)
    setFilePreview(getFilePathFromString(data))
  };

  const checkAnswerRadioAndCheckbox = (questionType, answerQuestion, answerStudent) => {
    if(questionType === 0 && answerQuestion != null){
      const answerQuestions = answerQuestion.split(',').sort();
      const answerStudents = answerStudent.split(',').sort();

      return answerStudents.every(elem => answerQuestions.includes(elem));
    }
  }

  const handleStartDoing = () => {
    // setStartLoadTime(new Date());
    setStartDoing(true)
  }

  return (
    <MyExamMonitor
      monitor={data?.examMonitor}
      blockScreen={data?.examBlockScreen}
      data={data}
      isCancel={isLoading}
    >
      <LocalizationProvider dateAdapter={AdapterDateFns}>
        <Card>
          <CardContent>
            <div style={{display: "flex", flexDirection: "column", alignItems: 'center', width: '100%'}}>
              <h1 style={{margin: 0, padding: 0}}>{data?.examName}</h1>
              <p style={{margin: 0, padding: 0}}>{parseHTMLToString(data?.examDescription)}</p>
              <h2 style={{margin: 0, padding: 0}}>{data?.examTestName}</h2>
              {/*<div style={{margin: 0, padding: 0, display: "flex"}}><span style={{fontWeight: "bold", marginRight: '5px'}}>Mã đề:</span>{data?.examTestCode}</div>*/}
              {/*<div style={{display: "flex"}}>*/}
              {/*  <p style={{margin: '0 20px 0 0', padding: 0, display: "flex"}}><span style={{fontWeight: "bold", marginRight: '5px'}}>Thời gian bắt đầu:</span>{formatDateTime(data?.startTime)}</p>*/}
              {/*  <p style={{margin: 0, padding: 0, display: "flex"}}><span style={{fontWeight: "bold", marginRight: '5px'}}>Thời gian kết thúc:</span>{formatDateTime(data?.endTime)}</p>*/}
              {/*</div>*/}
              {
                countdown > 0 && (
                  <div style={{display: "flex", justifyContent: 'flex-end', width: '100%'}}>
                    {/*Thời gian còn lại: <strong style={{fontSize: '16px'}}>{formatTimeToMMSS(countdown)}</strong>*/}
                    <div style={{position: 'relative', width: '100px', height: '100px'}}>
                      <svg width="100" height="100" viewBox="0 0 100 100">
                        <circle
                          cx="50"
                          cy="50"
                          r="45"
                          fill="none"
                          stroke="#e6e6e6"
                          strokeWidth="10"
                        />
                        <circle
                          cx="50"
                          cy="50"
                          r="45"
                          fill="none"
                          stroke="#ff4d4f"
                          strokeWidth="10"
                          strokeDasharray={strokeDasharray}
                          strokeDashoffset={strokeDashoffset}
                          transform="rotate(-90 50 50)"
                        />
                      </svg>
                      <div
                        style={{
                          position: 'absolute',
                          top: '50%',
                          left: '50%',
                          transform: 'translate(-50%, -50%)',
                          fontSize: '20px',
                          fontWeight: 'bold',
                          color: '#333',
                        }}
                      >
                        {formatTimeToMMSS(countdown)}
                      </div>
                    </div>
                  </div>
                )
              }
              {
                data?.submitedAt == null && !startDoing && (
                  <PrimaryButton
                    variant="contained"
                    color="primary"
                    style={{margin: "16px 0"}}
                    onClick={handleStartDoing}
                    type="submit"
                  >
                    Bắt đầu làm bài
                  </PrimaryButton>
                )
              }
            </div>

            {
              data?.submitedAt != null && (
                <div>
                  <div style={{
                    display: "flex",
                    width: '100%',
                    border: '2px solid #000000b8',
                    borderRadius: '10px',
                    margin: '10px 0'
                  }}>
                    <div style={{display: "flex", flexDirection: "column", width: '200px'}}>
                      <h3 style={{margin: 0, padding: '10px', borderBottom: '2px solid #000000b8'}}>Điểm</h3>
                      <p style={{
                        padding: 0,
                        margin: "auto",
                        height: '150px',
                        lineHeight: '150px',
                        fontWeight: "bold",
                        fontSize: '70px'
                      }}>{data?.totalScore}</p>
                    </div>
                    <div style={{
                      display: "flex",
                      flexDirection: "column",
                      borderLeft: '2px solid #000000b8',
                      width: 'calc(100% - 200px)'
                    }}>
                      <h3 style={{margin: 0, padding: '10px', borderBottom: '2px solid #000000b8'}}>Nhận xét</h3>
                      <p style={{padding: '0 10px', margin: 0, height: '150px'}}>{data?.comment ? parseHTMLToString(data?.comment) : ''}</p>
                    </div>
                  </div>
                  <div style={{display: "flex", alignItems:"center", marginBottom: '10px', justifyContent: "flex-end"}}>
                    <Timer/>
                    <p style={{padding: 0, margin: 0}}><strong>Tổng thời gian làm: </strong> {data?.totalTime} (phút)</p>
                  </div>
                  <div style={{display: "flex", alignItems:"center", marginBottom: '10px', justifyContent: "flex-end"}}>
                    <AccessTime/>
                    <p style={{padding: 0, margin: 0}}><strong>Thời gian nộp: </strong> {formatDateTime(data?.submitedAt)}</p>
                  </div>
                </div>
              )
            }

            {
              data?.questionList?.map(value => {
                const questionOrder = value?.questionOrder;
                return (
                  <div
                    key={value?.questionOrder}
                    style={{
                      border: '2px solid #f5f5f5',
                      borderColor:
                        (value?.questionType === 0 && data?.totalScore != null && data?.examAnswerStatus === 'OPEN') ?
                          (checkAnswerRadioAndCheckbox(value?.questionType, value?.questionAnswer, value?.answer) ? '#61bd6d' : '#f50000c9'):
                          '#f5f5f5',
                      display: 'flex',
                      justifyContent: 'space-between',
                      borderRadius: '10px',
                      padding: '10px',
                      marginBottom: '10px'
                    }}>
                    <Box display="flex"
                         flexDirection='column'
                         width="100%">

                      <div style={{display: "flex", justifyContent: "space-between"}}>
                        <div style={{display: 'flex'}}>
                        <span style={{
                          display: "block",
                          fontWeight: 'bold',
                          marginRight: '5px'
                        }}>Câu {value?.questionOrder}.</span>
                          <span
                            style={{fontStyle: 'italic'}}>({value?.questionType === 0 ? 'Trắc nghiệm' : 'Tự luận'})</span>
                        </div>

                        {
                          data?.totalScore != null && data?.examAnswerStatus === 'OPEN' && (
                            <div style={{display: "flex", alignItems: "center"}} key={questionOrder}>
                              {
                                value?.questionType === 0 ?
                                  (checkAnswerRadioAndCheckbox(value?.questionType, value?.questionAnswer, value?.answer) ?
                                    <CheckCircle style={{color: '#61bd6d'}}/> :
                                    <Cancel style={{color: '#f50000c9'}}/>) :
                                  (<></>)
                              }
                              <TextField
                                id={`scoreInput-${questionOrder}`}
                                label="Điểm"
                                style={{width: "60px", marginLeft: "16px"}}
                                size="small"
                                value={value?.score}
                                disabled
                                InputLabelProps={{
                                  shrink: true,
                                }}
                              />
                            </div>
                          )
                        }
                      </div>

                      <p><strong style={{marginRight: '10px'}}>Câu
                        hỏi: </strong>{parseHTMLToString(value?.questionContent)}</p>
                      {
                        value?.questionFile && (
                          value?.questionFile.split(';').map(item => {
                            return (
                              <div style={{display: 'flex', alignItems: 'center'}}>
                                <AttachFileOutlined></AttachFileOutlined>
                                <p style={{fontWeight: 'bold', cursor: 'pointer'}}
                                   onClick={() => handleOpenFilePreviewDialog(item)}>{getFilenameFromString(item)}</p>
                              </div>
                            )
                          })
                        )
                      }
                      {
                        value?.questionType === 0 && value?.questionMultichoice && (
                          <Box sx={{display: 'flex', flexDirection: 'column'}}>
                            <p style={{margin: 0, padding: 0, fontWeight: "bold"}}>Chọn các đáp án đúng trong các đáp án
                              sau:</p>
                            {
                              Array.from({ length: value?.questionNumberAnswer }, (_, index) => (
                                <FormControlLabel
                                  label={
                                    <FormGroup row>
                                      <Box display="flex" alignItems="center">
                                        <div>
                                          <p>{parseHTMLToString(value.questionAnswers[index]?.content)}</p>
                                          {value.questionAnswers[index]?.file && (
                                            <img src={getFilePathFromString(value.questionAnswers[index]?.file)} alt="" style={{maxHeight: "150px"}}/>
                                          )}
                                        </div>
                                        {( data?.totalScore != null && value?.questionAnswer?.includes(`${index+1}`)) && (<Check style={{ marginLeft: 8, color: 'green' }} />)}
                                      </Box>
                                    </FormGroup>
                                  }
                                  control={
                                    <Checkbox
                                      color="primary"
                                      checked={value?.answer?.includes(`${index+1}`)}
                                      disabled={data?.submitedAt != null}
                                      onChange={(event) => handleAnswerCheckboxChange(value?.questionOrder, `${index+1}`, event.target.checked)}
                                    />
                                  }
                                />
                              ))
                            }
                          </Box>
                        )
                      }
                      {
                        value?.questionType === 0 && !value?.questionMultichoice && (
                          <Box sx={{display: 'flex', flexDirection: 'column'}}>
                            <p style={{margin: 0, padding: 0, fontWeight: "bold"}}>Chọn đáp án đúng nhất:</p>
                            <RadioGroup
                              aria-labelledby="answer-radio-buttons-group-label"
                              name="answer-radio-buttons-group"
                              value={dataAnswers[value?.questionOrder - 1]?.answer}
                              onChange={(event) => handleAnswerRadioChange(event, value?.questionOrder)}
                            >
                              {
                                Array.from({ length: value?.questionNumberAnswer }, (_, index) => (
                                  <FormControlLabel
                                    value={`${index + 1}`}
                                    control={
                                      <Radio
                                        checked={value?.answer?.includes(`${index+1}`)}
                                        disabled={data?.submitedAt != null}
                                      />
                                    }
                                    label={
                                      <FormGroup row>
                                        <Box display="flex" alignItems="center">
                                          <div>
                                            <p>{parseHTMLToString(value.questionAnswers[index]?.content)}</p>
                                            {value.questionAnswers[index]?.file && (
                                              <img src={getFilePathFromString(value.questionAnswers[index]?.file)} alt="" style={{maxHeight: "150px"}}/>
                                            )}
                                          </div>
                                          {( data?.totalScore != null && value?.questionAnswer?.includes(`${index+1}`)) && (<Check style={{ marginLeft: 8, color: 'green' }} />)}
                                        </Box>
                                      </FormGroup>
                                    }
                                  />
                                ))
                              }
                            </RadioGroup>
                          </Box>
                        )
                      }
                      {
                        value?.questionType === 1 && (
                          <div key={questionOrder}>
                            {
                              data?.submitedAt == null && startDoing && (
                                <div>
                                  <RichTextEditor
                                    content={tmpTextAnswer}
                                    onContentChange={(value) =>
                                      handleAnswerTextChange(value, questionOrder)
                                    }
                                  />
                                  <DropzoneArea
                                    dropzoneClass={classes.dropZone}
                                    filesLimit={20}
                                    maxFileSize={10000000}
                                    showPreviews={true}
                                    showPreviewsInDropzone={false}
                                    useChipsForPreview
                                    dropzoneText={`Kéo và thả tệp vào đây hoặc nhấn để chọn tệp cho Câu hỏi số ${questionOrder}`}
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
                                    onChange={(files) => handleAnswerFileChange(files, questionOrder)}
                                  ></DropzoneArea>
                                </div>
                              )
                            }
                            {
                              data?.submitedAt != null && (
                                <div><strong style={{marginRight: '10px'}}>Trả
                                  lời:</strong>{parseHTMLToString(value?.answer)}</div>
                              )
                            }
                            {
                              data?.submitedAt != null && value?.filePathAnswer != null && value?.filePathAnswer !== '' && (
                                <div style={{marginTop: '10px'}}>
                                  <strong>File trả lời đính kèm:</strong>
                                  {
                                    value?.filePathAnswer.split(';').map(item => {
                                      return (
                                        <div>
                                          <div style={{display: 'flex', alignItems: 'center'}}>
                                            <AttachFileOutlined></AttachFileOutlined>
                                            <p style={{fontWeight: 'bold', cursor: 'pointer'}}
                                               onClick={() => handleOpenFilePreviewDialog(item)}>{getFilenameFromString(item)}</p>
                                          </div>
                                          {
                                            value?.filePathComment &&
                                            getFileCommentFromFileAnswerAndExamResultDetailsId(value?.filePathComment, item, value?.examResultDetailsId) &&
                                            data?.totalScore != null && data?.examAnswerStatus === 'OPEN' && (
                                              <div style={{display: 'flex', alignItems: 'center', marginLeft: "22px"}}>
                                                <Comment style={{color: 'green'}}/>
                                                <p style={{color: 'green', fontWeight: 'bold', cursor: 'pointer', margin: "0 3px"}}
                                                   onClick={() => handleOpenFilePreviewDialog(getFileCommentFromFileAnswerAndExamResultDetailsId(value?.filePathComment, item, value?.examResultDetailsId))}
                                                >Xem nhận xét</p>
                                              </div>
                                            )
                                          }
                                        </div>
                                      )
                                    })
                                  }
                                </div>
                              )
                            }
                            {
                              data?.totalScore != null && data?.examAnswerStatus === 'OPEN' && (
                                <div>
                                  <strong style={{marginRight: '10px'}}>Đáp án:</strong>{parseHTMLToString(value?.questionAnswer)}
                                </div>
                              )
                            }
                          </div>
                        )
                      }
                      {
                        data?.totalScore != null && data?.examAnswerStatus === 'OPEN' && (
                          <div>
                            <strong style={{marginRight: '10px'}}>Giải thích:</strong>{parseHTMLToString(value?.questionExplain)}
                          </div>
                        )
                      }
                    </Box>
                  </div>
                )
              })
            }

            {/*{*/}
            {/*  data?.submitedAt == null && (*/}
            {/*    <DropzoneArea*/}
            {/*      dropzoneClass={classes.dropZone}*/}
            {/*      filesLimit={20}*/}
            {/*      showPreviews={true}*/}
            {/*      showPreviewsInDropzone={false}*/}
            {/*      useChipsForPreview*/}
            {/*      dropzoneText={`Kéo và thả tệp vào đây hoặc nhấn để chọn tệp cho bài thi`}*/}
            {/*      previewText="Xem trước:"*/}
            {/*      previewChipProps={{*/}
            {/*        variant: "outlined",*/}
            {/*        color: "primary",*/}
            {/*        size: "medium",*/}
            {/*      }}*/}
            {/*      getFileAddedMessage={(fileName) =>*/}
            {/*        `Tệp ${fileName} tải lên thành công`*/}
            {/*      }*/}
            {/*      getFileRemovedMessage={(fileName) => `Tệp ${fileName} đã loại bỏ`}*/}
            {/*      getFileLimitExceedMessage={(filesLimit) =>*/}
            {/*        `Vượt quá số lượng tệp tối đa được cho phép. Chỉ được phép tải lên tối đa ${filesLimit} tệp.`*/}
            {/*      }*/}
            {/*      alertSnackbarProps={{*/}
            {/*        anchorOrigin: {vertical: "bottom", horizontal: "right"},*/}
            {/*        autoHideDuration: 1800,*/}
            {/*      }}*/}
            {/*      onChange={(files) => setAnswersFiles(files)}*/}
            {/*    ></DropzoneArea>*/}
            {/*  )*/}
            {/*}*/}

            {/*{*/}
            {/*  data?.submitedAt != null && (*/}
            {/*    <div>*/}
            {/*      <h4 style={{marginBottom: 0, fontSize: '18px'}}>File đính kèm:</h4>*/}
            {/*      {*/}
            {/*        (data?.answerFiles == null || data?.answerFiles == '') ?*/}
            {/*          (*/}
            {/*            <div>N/A</div>*/}
            {/*          ) :*/}
            {/*          (*/}
            {/*            data?.answerFiles.split(';').map(item => {*/}
            {/*              return (*/}
            {/*                <div style={{display: 'flex', alignItems: 'center'}}>*/}
            {/*                  <AttachFileOutlined></AttachFileOutlined>*/}
            {/*                  <p style={{fontWeight: 'bold', cursor: 'pointer'}}*/}
            {/*                     onClick={() => handleOpenFilePreviewDialog(item)}>{getFilenameFromString(item)}</p>*/}
            {/*                </div>*/}
            {/*              )*/}
            {/*            })*/}
            {/*          )*/}
            {/*      }*/}
            {/*    </div>*/}
            {/*  )*/}
            {/*}*/}

          </CardContent>
          <CardActions style={{justifyContent: 'flex-end'}}>
            {
              !(data?.examMonitor && data?.examMonitor > 0 && data?.submitedAt == null) && (
                <TertiaryButton
                  variant="outlined"
                  onClick={() => {
                    history.push("/exam/my-exam")
                    openMenu()
                  }}
                >
                  Hủy
                </TertiaryButton>
              )
            }
            {
              data?.submitedAt == null && startDoing && (
                <PrimaryButton
                  disabled={isLoading}
                  variant="contained"
                  color="primary"
                  style={{marginLeft: "15px"}}
                  onClick={handleSubmit}
                  type="submit"
                >
                  {isLoading ? <CircularProgress/> : "Nộp bài"}
                </PrimaryButton>
              )
            }
          </CardActions>
        </Card>
        <QuestionFilePreview
          open={openFilePreviewDialog}
          setOpen={setOpenFilePreviewDialog}
          file={filePreview}>
        </QuestionFilePreview>
      </LocalizationProvider>
    </MyExamMonitor>
  );
}

const screenName = "MENU_EXAMINEE_PARTICIPANT";
//export default withScreenSecurity(MyExamDetails, screenName, true);
export default MyExamDetails;
