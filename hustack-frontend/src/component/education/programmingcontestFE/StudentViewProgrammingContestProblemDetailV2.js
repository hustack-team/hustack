import {LoadingButton} from "@mui/lab";
import {Alert, Box, Button, Divider, Stack, Typography} from "@mui/material";
import {styled} from "@mui/material/styles";
import HustCopyCodeBlock from "component/common/HustCopyCodeBlock";
import HustModal from "component/common/HustModal";
import {ContentState, EditorState} from "draft-js";
import htmlToDraft from "html-to-draftjs";
import React, {forwardRef, useEffect, useRef, useState} from "react";
import {Editor} from "react-draft-wysiwyg";
import {useParams} from "react-router";
import {randomImageName} from "utils/FileUpload/covert";
import {errorNoti, successNoti} from "utils/notification";
import {request} from "../../../api";
import FileUploadZone from "../../../utils/FileUpload/FileUploadZone";
import HustCodeEditor from "../../common/HustCodeEditor";
import HustCodeLanguagePicker from "../../common/HustCodeLanguagePicker";
import {
  COMPUTER_LANGUAGES,
  DEFAULT_CODE_SEGMENT_C,
  DEFAULT_CODE_SEGMENT_CPP,
  DEFAULT_CODE_SEGMENT_JAVA,
  DEFAULT_CODE_SEGMENT_PYTHON,
  SUBMISSION_MODE_NOT_ALLOWED,
  SUBMISSION_MODE_SOURCE_CODE,
} from "./Constant";
import StudentViewSubmission from "./StudentViewSubmission";
import {useTranslation} from "react-i18next";
import _ from "lodash";
import ProgrammingContestLayout from "./ProgrammingContestLayout";
import {useHistory} from "react-router-dom";

const VisuallyHiddenInput = styled("input")({
  clip: "rect(0 0 0 0)",
  clipPath: "inset(50%)",
  height: 1,
  overflow: "hidden",
  position: "absolute",
  bottom: 0,
  left: 0,
  whiteSpace: "nowrap",
  width: 1,
});

export const InputFileUpload = forwardRef((props, ref) => {
  const {label, buttonProps, ...otherProps} = props;
  return (
    <Button
      component="label"
      variant="outlined"
      sx={{textTransform: "none"}}
      {...buttonProps}
    >
      {label}
      <VisuallyHiddenInput type="file" ref={ref} {...otherProps} />
    </Button>
  );
});

const editorStyle = {
  editor: {
    // border: "1px solid black",
    // minHeight: "300px",
  },
};

const ERR_STATUS = [
  "NOT_FOUND",
  "NOT_ALLOWED_TO_SUBMIT",
  "NOT_ALLOWED_LANGUAGE",
  "PARTICIPANT_NOT_REGISTERED_OR_APPROVED",
  "PARTICIPANT_HAS_NO_PERMISSION_TO_SUBMIT",
  "SUBMISSION_NOT_ALLOWED",
  "MAX_NUMBER_SUBMISSIONS_REACHED",
  "SOURCE_CODE_REQUIRED",
  "MAX_SOURCE_CODE_LENGTH_VIOLATIONS",
  "SUBMISSION_INTERVAL_VIOLATIONS",
];

export default function StudentViewProgrammingContestProblemDetail() {
  const {problemId, contestId} = useParams();
  const history = useHistory();
  const {t} = useTranslation(["education/programmingcontest/problem", "common"]);

  const [problem, setProblem] = useState(null);
  const [testCases, setTestCases] = useState([]);
  const [file, setFile] = useState(null);
  const [language, setLanguage] = useState(COMPUTER_LANGUAGES.CPP17);
  const [listLanguagesAllowed, setListLanguagesAllowed] = useState([]);
  // const [status, setStatus] = useState("");
  // const [message, setMessage] = useState("");
  const [codeSolution, setCodeSolution] = useState("");
  const [submissionMode, setSubmissionMode] = useState(
    SUBMISSION_MODE_SOURCE_CODE
  );
  const [isSubmitCode, setIsSubmitCode] = useState(0);

  const [openModalPreview, setOpenModalPreview] = useState(false);
  const [selectedTestcase, setSelectedTestcase] = useState();
  const [isProcessing, setIsProcessing] = React.useState(false);
  // const [problemDescription, setProblemDescription] = useState(
  //   ""
  // );
  const [editorStateDescription, setEditorStateDescription] = useState(
    EditorState.createEmpty()
  );
  const [sampleTestCase, setSampleTestCase] = useState(
    null//EditorState.createEmpty()
  );

  const [fetchedImageArray, setFetchedImageArray] = useState([]);

  const inputRef = useRef();
  const listSubmissionRef = useRef(null);

  function onFileChange(event) {
    setFile(event.target.files[0]);
  }

  const onInputChange = (event) => {
    let name = event.target.value;
    setFile(name);
  };

  async function isFileBlank(file) {
    if (!file) return true;

    const content = await readFileAsText(file);
    return _.isEmpty(_.trim(content));
  }

  function readFileAsText(file) {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = (e) => resolve(e.target.result);
      reader.onerror = (e) => {
        errorNoti(tTestcase("errorReadingFile"));
        ;reject(e)
      };
      reader.readAsText(file);
    });
  }

  const handleFormSubmit = async (event) => {
    if (event) event.preventDefault();
    setIsProcessing(true);

    const body = {
      problemId: problemId,
      contestId: contestId,
      language: language,
    };

    if (await isFileBlank(file)) {
      errorNoti("Source code is required", 3000);
      setIsProcessing(false);
      return;
    }

    const formData = new FormData();
    formData.append("dto", new Blob([JSON.stringify(body)], {type: 'application/json'}));
    formData.append("file", file);

    const config = {
      headers: {
        "content-Type": "multipart/form-data",
      },
    };

    //TODO: consider remove duplicate code
    request(
      "post",
      "/submissions/file-upload",
      (res) => {
        setIsProcessing(false);
        res = res.data;
        listSubmissionRef.current.refreshSubmission();
        inputRef.current.value = null;

        if (ERR_STATUS.includes(res.status)) {
          errorNoti(res.message, 3000);
        } else {
          successNoti("Submitted", 3000);
        }

        // setStatus(res.status);
        // setMessage(res.message);

        setFile(null);
        inputRef.current.value = null;
      },
      {
        onError: (e) => {
          setIsProcessing(false);
          setFile(null);
          inputRef.current.value = null;

          errorNoti(t("common:error", 3000))
        },
      },
      formData,
      config
    );
  };

  function getProblemDetail() {
    request(
      "get",
      "/contests/" + contestId + "/problems/" + problemId,
      (res) => {
        res = res.data;
        setProblem(res);
        if (res.listLanguagesAllowed != null && res.listLanguagesAllowed.length > 0) {
          setLanguage(res.listLanguagesAllowed[0])
          setListLanguagesAllowed(res.listLanguagesAllowed);
        }
        if (res.isPreloadCode) setCodeSolution(res.preloadCode);
        if (res.submissionMode) setSubmissionMode(res.submissionMode);
        if (res.attachment && res.attachment.length !== 0) {
          const newFileURLArray = res.attachment.map((url) => ({
            id: randomImageName(),
            content: url,
          }));
          newFileURLArray.forEach((file, idx) => {
            file.fileName = res.attachmentNames[idx];
          });
          setFetchedImageArray(newFileURLArray);
        }

        // setProblemDescription(res?.problemStatement || "");
        let problemDescriptionHtml = htmlToDraft(res.problemStatement);
        let {contentBlocks, entityMap} = problemDescriptionHtml;
        let contentDescriptionState = ContentState.createFromBlockArray(
          contentBlocks,
          entityMap
        );
        let statementDescription = EditorState.createWithContent(
          contentDescriptionState
        );
        setEditorStateDescription(statementDescription);

        // public testcase    
        /*  
        let sampleTestCaseHtml = htmlToDraft(res.sampleTestCase);
        let { contentBlocksTestCase, entityMapTestCase } = sampleTestCaseHtml;
        let contentDescriptionStateTestCase = ContentState.createFromBlockArray(
          contentBlocksTestCase,
          entityMapTestCase
        );
        let editorSampleTestCase = EditorState.createWithContent(
          contentDescriptionStateTestCase
        );
        //setSampleTestCase(editorSampleTestCase);
        */
        setSampleTestCase(res.sampleTestCase);
        //console.log('GetProblemDetail, res = ',res);
      },
      {onError: (e) => console.log(e)}
    );
  }

  useEffect(() => {
    getProblemDetail();
  }, []);

  useEffect(() => {
    if (problem && problem.isPreloadCode === true) return;
    switch (language) {
      case COMPUTER_LANGUAGES.C:
        setCodeSolution(DEFAULT_CODE_SEGMENT_C);
        break;
      case COMPUTER_LANGUAGES.CPP11:
      case COMPUTER_LANGUAGES.CPP14:
      case COMPUTER_LANGUAGES.CPP17:
        setCodeSolution(DEFAULT_CODE_SEGMENT_CPP);
        break;
      case COMPUTER_LANGUAGES.JAVA:
        setCodeSolution(DEFAULT_CODE_SEGMENT_JAVA);
        break;
      case COMPUTER_LANGUAGES.PYTHON:
        setCodeSolution(DEFAULT_CODE_SEGMENT_PYTHON);
        break;
    }
  }, [language]);

  const ModalPreview = (chosenTestcase) => {
    return (
      <HustModal
        open={openModalPreview}
        onClose={() => setOpenModalPreview(false)}
        isNotShowCloseButton
        showCloseBtnTitle={false}
      >
        <HustCopyCodeBlock
          title="Input"
          text={chosenTestcase?.chosenTestcase?.testCase}
        />
        <HustCopyCodeBlock
          title="Output"
          text={chosenTestcase?.chosenTestcase?.correctAns}
          mt={2}
        />
      </HustModal>
    );
  };

  async function submitCode() {
    const blob = new Blob([codeSolution], {type: "text/plain;charset=utf-8"});
    const now = new Date();
    const file = new File(
      [blob],
      `${problemId}_${now.getTime()}.txt`,
      {type: "text/plain;charset=utf-8"}
    );
    setFile(file);
    setIsSubmitCode(isSubmitCode + 1);
  }

  useEffect(() => {
    if (isSubmitCode > 0) handleFormSubmit(null);
  }, [isSubmitCode]);

  const handleExit = () => {
    history.push(`/programming-contest/student-view-contest-detail/${contestId}`);
  }

  return (
    <ProgrammingContestLayout title={problem ? problem.problemName : ""} onBack={handleExit}>
      <Typography variant="h6" sx={{mb: 1}}>
        {t("common:description")}
      </Typography>
      {/*{ReactHtmlParser(problemDescription)}*/}
      <Editor
        toolbarHidden
        editorState={editorStateDescription}
        handlePastedText={() => false}
        readOnly
        editorStyle={editorStyle.editor}
      />
      {/*
        <Typography variant="h5">Sample testcase</Typography>
        
        <Editor
          toolbarHidden
          editorState={sampleTestCase}
          handlePastedText={() => false}
          readOnly
          editorStyle={editorStyle.editor}
        />
      */}
      {/*ReactHtmlParser(sampleTestCase)*/}
      {/*sampleTestCase*/}

      {sampleTestCase && <>
        <div style={{height: "10px"}}></div>
        <HustCopyCodeBlock
          title={t("sampleTestCase")}
          text={sampleTestCase}
        />
      </>}

      {fetchedImageArray.length !== 0 &&
        fetchedImageArray.map((file) => (
          <FileUploadZone file={file} removable={false}/>
        ))}

      <ModalPreview chosenTestcase={selectedTestcase}/>

      <Box sx={{mt: 2}}>
        <Box>
          <HustCodeEditor
            title={t('common:sourceCode')}
            language={language}
            onChangeLanguage={(event) => {
              setLanguage(event.target.value);
            }}
            sourceCode={codeSolution}
            onChangeSourceCode={(code) => {
              setCodeSolution(code);
            }}
            height={"480px"}
            listLanguagesAllowed={listLanguagesAllowed}
          />
          <Box
            sx={{
              width: "100%",
              display: "flex",
              justifyContent: "center",
              alignItems: "center",
            }}
          >
            <LoadingButton
              disabled={
                isProcessing || submissionMode === SUBMISSION_MODE_NOT_ALLOWED
              }
              sx={{width: 128, mt: 1, mb: 1, textTransform: 'none'}}
              // loading={isProcessing}
              // loadingIndicator="Submitting…"
              variant="contained"
              color="primary"
              type="submit"
              onClick={submitCode}
            >
              {t("common:submit")}
            </LoadingButton>

            {submissionMode === SUBMISSION_MODE_NOT_ALLOWED && (
              <Typography color="gray" ml={1}>
                Currently, this contest problem is not open for submissions
              </Typography>
            )}
          </Box>
        </Box>

        <Divider>Or</Divider>

        <form onSubmit={handleFormSubmit}>
          <Stack alignItems={"center"} spacing={2} sx={{mt: 1}}>
            <Stack
              direction="row"
              justifyContent={"center"}
              alignItems="center"
              spacing={4}
            >
              <HustCodeLanguagePicker
                listLanguagesAllowed={listLanguagesAllowed}
                language={language}
                onChangeLanguage={(e) => setLanguage(e.target.value)}
              />
              <Stack direction="row" spacing={1} alignItems="center">
                <InputFileUpload
                  id="selected-upload-file"
                  label={t("common:selectFile")}
                  accept=".c, .cpp, .java, .py"
                  onChange={onFileChange}
                  ref={inputRef}
                />
                {file && <Typography variant="body1">{file.name}</Typography>}
              </Stack>
            </Stack>

            <LoadingButton
              disabled={
                isProcessing || submissionMode === SUBMISSION_MODE_NOT_ALLOWED
              }
              sx={{width: 128, textTransform: 'none'}}
              // loading={isProcessing}
              // loadingIndicator="Submitting…"
              variant="contained"
              color="primary"
              type="submit"
              onChange={onInputChange}
            >
              {t("common:submit")}
            </LoadingButton>
          </Stack>
        </form>
        {/* <div>
          <h3>
            Status: <em>{status}</em>
          </h3>
        </div>
        <div>
          <h3>
            Message: <em>{message}</em>
          </h3>
        </div> */}
      </Box>
      {language === COMPUTER_LANGUAGES.JAVA && (
        <Alert
          variant="outlined"
          severity="info"
          sx={{
            borderRadius: 1.5,
            bgcolor: "#e5f6fd",
            mt: 3,
          }}
        >
          With Java, the public class must be declared as:{" "}
          <b>public class Main {"{...}"}</b>
        </Alert>
      )}
      <Box sx={{mt: 3}}>
        <StudentViewSubmission problemId={problemId} ref={listSubmissionRef}/>
      </Box>
    </ProgrammingContestLayout>
  );
}
