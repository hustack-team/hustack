import EditIcon from "@mui/icons-material/Edit";
import { makeStyles } from "@material-ui/core/styles";
import {
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Grid,
  LinearProgress,
  Stack,
  TextField,
  Typography,
  Tab,
  IconButton,
  Collapse,
  FormControlLabel,
  Switch,
} from "@mui/material";
import { request } from "api";
import withScreenSecurity from "component/withScreenSecurity";
import { useEffect, useState } from "react";
import "react-draft-wysiwyg/dist/react-draft-wysiwyg.css";
import { useTranslation } from "react-i18next";
import { useHistory, useParams } from "react-router-dom";
import FileUploadZone from "utils/FileUpload/FileUploadZone";
import { randomImageName } from "utils/FileUpload/covert";
import { PROBLEM_ROLE, PROBLEM_STATUS } from "utils/constants";
import RichTextEditor from "../../common/editor/RichTextEditor";
import { COMPUTER_LANGUAGES, CUSTOM_EVALUATION, mapLanguageToCodeBlockLanguage } from "./Constant";
import ContestsUsingAProblem from "./ContestsUsingAProblem";
import ListTestCase from "./ListTestCase";
import { localeOption } from "utils/NumberFormat";
import { detail } from "./ContestProblemSubmissionDetailViewedByManager";
import ProgrammingContestLayout from "./ProgrammingContestLayout";
import PrimaryButton from "../../button/PrimaryButton";
import TertiaryButton from "../../button/TertiaryButton";
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import { getLevels, getStatuses } from "./CreateProblem";
import HustCopyCodeBlock from "../../common/HustCopyCodeBlock";
import { StyledTabs } from "component/tab";
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';

const useStyles = makeStyles((theme) => ({
  blockCode: {
    border: '2px solid black',
    padding: theme.spacing(2),
    marginTop: theme.spacing(2),
    position: 'relative',
  },
  blockCodeStudent: {
    border: '2px solid #4CAF50',
    padding: theme.spacing(2),
    marginTop: theme.spacing(2),
    position: 'relative',
    backgroundColor: '#E8F5E9',
  },
  forStudentLabel: {
    position: 'absolute',
    top: theme.spacing(1),
    right: theme.spacing(1),
    fontSize: '0.875rem',
    color: theme.palette.text.secondary,
  },
  expandIcon: {
    transition: theme.transitions.create('transform', {
      duration: theme.transitions.duration.shortest,
    }),
  },
  expandIconOpen: {
    transform: 'rotate(180deg)',
  },
}));

const PROGRAMMING_LANGUAGES = Object.keys(COMPUTER_LANGUAGES).map((key) => ({
  label: key,
  value: COMPUTER_LANGUAGES[key],
}));

function ManagerViewProblemDetailV2() {
  const { problemId } = useParams();
  const history = useHistory();
  const classes = useStyles();

  const { t } = useTranslation([
    "education/programmingcontest/problem",
    "common",
    "validation",
  ]);

  const [fetchedImageArray, setFetchedImageArray] = useState([]);
  const [openCloneDialog, setOpenCloneDialog] = useState(false);
  const [newProblemId, setNewProblemId] = useState("");
  const [newProblemName, setNewProblemName] = useState("");
  const [errorMessage, setErrorMessage] = useState("");
  const [loading, setLoading] = useState(true);

  const [problemDetail, setProblemDetail] = useState({
    problemName: "",
    description: "",
    timeLimitCPP: null,
    timeLimitJAVA: null,
    timeLimitPYTHON: null,
    memoryLimit: null,
    levelId: "",
    correctSolutionLanguage: COMPUTER_LANGUAGES.CPP17,
    correctSolutionSourceCode: "",
    isPreloadCode: false,
    preloadCode: "",
    solutionCheckerSourceLanguage: COMPUTER_LANGUAGES.CPP17,
    solutionCheckerSourceCode: "",
    isCustomEvaluated: false,
    public: false,
    tags: [],
    status: "",
    roles: [],
    sampleTestCase: null,
    categoryId: 0,
  });
  const [blockCodes, setBlockCodes] = useState(
    Object.fromEntries(PROGRAMMING_LANGUAGES.map(({ value }) => [value, []]))
  );
  const [selectedLanguage, setSelectedLanguage] = useState(COMPUTER_LANGUAGES.CPP17);
  const [isBlockCodesExpanded, setIsBlockCodesExpanded] = useState(false);
  const [blockDisplayMode, setBlockDisplayMode] = useState("individual");

  const handleExit = () => {
    history.push(`/programming-contest/list-problems`);
  };

  useEffect(() => {
    request("get", "teacher/problems/" + problemId, (res) => {
      setLoading(false);
      const data = res.data;

      if (data.attachment && data.attachment.length !== 0) {
        const newFileURLArray = data.attachment.map((url) => ({
          id: randomImageName(),
          content: url,
        }));
        newFileURLArray.forEach((file, idx) => {
          file.fileName = data.attachmentNames[idx];
        });
        setFetchedImageArray(newFileURLArray);
      }

      setProblemDetail({
        ...data,
        public: data.publicProblem,
        solutionCheckerSourceCode: data.solutionCheckerSourceCode || "",
        isCustomEvaluated: data.scoreEvaluationType === CUSTOM_EVALUATION,
        description: data.problemDescription,
        categoryId: data.categoryId || 0,
      });

      if (data.categoryId > 0) {
        const newBlockCodes = Object.fromEntries(
          PROGRAMMING_LANGUAGES.map(({ value }) => [value, []])
        );
        data.blockCodes.forEach((block) => {
          if (newBlockCodes[block.language]) {
            newBlockCodes[block.language].push({
              id: block.id,
              code: block.code,
              forStudent: block.forStudent,
              seq: block.seq,
            });
          }
        });
        Object.keys(newBlockCodes).forEach((lang) => {
          newBlockCodes[lang].sort((a, b) => a.seq - b.seq);
        });
        setBlockCodes(newBlockCodes);
      }
    });
  }, [problemId]);

  const hasSpecialCharacterProblemId = () => {
    return !new RegExp(/^[0-9a-zA-Z_-]*$/).test(newProblemId);
  };

  const hasSpecialCharacterProblemName = () => {
    return !new RegExp(/^[0-9a-zA-Z ]*$/).test(newProblemName);
  };

  const handleCloneDialogOpen = () => {
    setOpenCloneDialog(true);
  };

  const handleCloneDialogClose = () => {
    setOpenCloneDialog(false);
    setNewProblemId("");
    setNewProblemName("");
    setErrorMessage("");
  };

  const handleClone = () => {
    if (hasSpecialCharacterProblemId()) {
      setErrorMessage("Problem ID can only contain letters, numbers, underscores, and hyphens.");
      return;
    }
    if (hasSpecialCharacterProblemName()) {
      setErrorMessage("Problem Name can only contain letters and numbers.");
      return;
    }

    const cloneRequest = {
      oldProblemId: problemId,
      newProblemId: newProblemId,
      newProblemName: newProblemName,
    };

    request(
      "post",
      "/teachers/problems/clone",
      (res) => {
        handleCloneDialogClose();
        history.push("/programming-contest/list-problems");
      },
      {
        onError: (error) => {
          setErrorMessage("Failed to clone the problem. Please try again.");
          console.error("Error cloning problem:", error);
        },
        400: (error) => {
          setErrorMessage("Invalid request. Please check your input.");
        },
        404: (error) => {
          setErrorMessage("Original problem not found.");
        },
        500: (error) => {
          setErrorMessage("Original problem already exists.");
        },
      },
      cloneRequest
    );
  };

  const handleTabChange = (event, newValue) => {
    setSelectedLanguage(newValue);
  };

  const handleToggleBlockDisplayMode = () => {
    setBlockDisplayMode((prev) => (prev === "individual" ? "combined" : "individual"));
  };

  const getCombinedBlockCode = () => {
    const blocks = blockCodes[selectedLanguage] || [];
    if (blocks.length === 0) return "// No blocks available";
    return blocks
      .map((block, index) => `// --- Block ${block.seq} (${block.forStudent ? t("forStudent") : t("forTeacher")}) ---\n${block.code}`)
      .join("\n\n");
  };

  return (
    <ProgrammingContestLayout title={t("viewProblem")} onBack={handleExit}>
      <Stack direction="row" spacing={2} mb={1.5} justifyContent="space-between">
        <Typography variant="h6" component='span'>
          {t("generalInfo")}
        </Typography>

        <Stack direction="row" spacing={2}>
          {(!problemDetail.roles.includes(PROBLEM_ROLE.OWNER) &&
            (!problemDetail.roles.includes(PROBLEM_ROLE.EDITOR) || problemDetail.status !== PROBLEM_STATUS.OPEN)
          ) ? null : (
            <PrimaryButton
              onClick={() => {
                history.push("/programming-contest/edit-problem/" + problemId);
              }}
              startIcon={<EditIcon />}
            >
              {t("common:edit", { name: '' })}
            </PrimaryButton>
          )}
          {(!problemDetail.roles.includes(PROBLEM_ROLE.OWNER) &&
            (!problemDetail.roles.includes(PROBLEM_ROLE.EDITOR) ||
              problemDetail.status !== PROBLEM_STATUS.OPEN)) ? null : (
            <TertiaryButton
              variant="outlined"
              onClick={handleCloneDialogOpen}
              startIcon={<ContentCopyIcon />}
            >
              {t("clone")}
            </TertiaryButton>
          )}
          {problemDetail.roles.includes(PROBLEM_ROLE.OWNER) && (
            <TertiaryButton
              variant="outlined"
              onClick={() => {
                history.push(
                  "/programming-contest/user-contest-problem-role-management/" +
                  problemId
                );
              }}
            >
              {t("manageRole")}
            </TertiaryButton>
          )}
        </Stack>
      </Stack>

      <Dialog open={openCloneDialog} onClose={handleCloneDialogClose}>
        <DialogTitle>{"Clone Problem"}</DialogTitle>
        <DialogContent>
          <TextField
            autoFocus
            margin="dense"
            label="New Problem ID"
            type="text"
            fullWidth
            variant="outlined"
            value={newProblemId}
            onChange={(e) => setNewProblemId(e.target.value)}
            error={hasSpecialCharacterProblemId()}
            helperText={hasSpecialCharacterProblemId() ? "Invalid characters in Problem ID." : ""}
          />
          <TextField
            margin="dense"
            label="New Problem Name"
            type="text"
            fullWidth
            variant="outlined"
            value={newProblemName}
            onChange={(e) => setNewProblemName(e.target.value)}
            helperText={""}
          />
          {errorMessage && <Typography color="error">{errorMessage}</Typography>}
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloneDialogClose} color="primary">
            Cancel
          </Button>
          <Button onClick={handleClone} color="primary">
            Create
          </Button>
        </DialogActions>
      </Dialog>

      {loading && <LinearProgress />}
      <Grid container spacing={2} display={loading ? "none" : ""}>
        {[
          [t("problemName"), problemDetail.problemName],
          [t("level"), getLevels(t).find(item => item.value === problemDetail.levelId)?.label],
          [t("status"), getStatuses(t).find(item => item.value === problemDetail.status)?.label],
          [
            t("public", { ns: "common" }),
            problemDetail.public ? t("common:yes") : t("common:no"),
          ],
          [
            t("timeLimit") + ' C/CPP',
            problemDetail.timeLimitCPP ? `${problemDetail.timeLimitCPP.toLocaleString(
              "fr-FR",
              localeOption
            )} (s)` : null,
          ],
          [
            t("timeLimit") + ' Java',
            problemDetail.timeLimitJAVA ? `${problemDetail.timeLimitJAVA.toLocaleString(
              "fr-FR",
              localeOption
            )} (s)` : null,
          ],
          [
            t("timeLimit") + ' Python',
            problemDetail.timeLimitPYTHON ? `${problemDetail.timeLimitPYTHON.toLocaleString(
              "fr-FR",
              localeOption
            )} (s)` : null,
          ],
          [
            t("memoryLimit"),
            problemDetail.memoryLimit ? `${problemDetail.memoryLimit.toLocaleString(
              "fr-FR",
              localeOption
            )} (MB)` : null,
          ],
          [
            t("tag"),
            problemDetail.tags
              ? problemDetail.tags.map((selectedTag) => selectedTag.name).join(", ") : null,
          ],
        ].map(([key, value, sx, helpText]) => (
          <Grid item xs={12} sm={12} md={3} key={key}>
            {detail(key, value, sx, helpText)}
          </Grid>
        ))}
      </Grid>

      <Box sx={{ marginTop: "24px", marginBottom: "24px" }}>
        <Typography variant="h6" sx={{ marginBottom: "8px" }}>
          {t("common:description")}
        </Typography>
        <RichTextEditor
          toolbarHidden
          content={problemDetail.description}
          readOnly
          editorStyle={{ editor: {} }}
        />
      </Box>

      {problemDetail.sampleTestCase && (
        <HustCopyCodeBlock title={t("sampleTestCase")} text={problemDetail.sampleTestCase} />
      )}

      {fetchedImageArray.length !== 0 &&
        fetchedImageArray.map((file) => (
          <FileUploadZone key={file.id} file={file} removable={false} />
        ))}

      {problemDetail.categoryId > 0 && (
        <Box sx={{ marginTop: "24px" }}>
          <Typography variant="h6" sx={{ marginBottom: "8px" }}>
            {t("listProblemBlock")}
          </Typography>
          <Box sx={{ display: "flex", alignItems: "center", marginBottom: "12px" }}>
            <IconButton
              onClick={() => setIsBlockCodesExpanded(!isBlockCodesExpanded)}
              aria-expanded={isBlockCodesExpanded}
              aria-label={t("common:toggleBlockCodes")}
              style={{ color: '#00bcd4' }}
              size="small"
            >
              <ExpandMoreIcon
                className={`${classes.expandIcon} ${isBlockCodesExpanded ? classes.expandIconOpen : ""}`}
              />
            </IconButton>
            <Typography variant="body1">{t("common:toggleBlockCodes")}</Typography>
          </Box>
          <Collapse in={isBlockCodesExpanded}>
            <Box sx={{ display: "flex", alignItems: "center", marginBottom: "12px" }}>
              <FormControlLabel
                control={
                  <Switch
                    checked={blockDisplayMode === "combined"}
                    onChange={handleToggleBlockDisplayMode}
                  />
                }
                label={
                  blockDisplayMode === "combined"
                    ? t("common:individualBlocks")
                    : t("common:combinedBlock")
                }
              />
            </Box>
            <StyledTabs
              value={selectedLanguage}
              onChange={handleTabChange}
              sx={{ marginBottom: "12px" }}
            >
              {PROGRAMMING_LANGUAGES.map((lang) => (
                <Tab key={lang.value} label={lang.label} value={lang.value} />
              ))}
            </StyledTabs>
            {blockDisplayMode === "individual" ? (
              blockCodes[selectedLanguage].length > 0 ? (
                blockCodes[selectedLanguage].map((block, index) => (
                  <Box
                    key={block.id || index}
                    className={block.forStudent ? classes.blockCodeStudent : classes.blockCode}
                  >
                    <Typography className={classes.forStudentLabel}>
                      {block.forStudent ? t("forStudent") : t("forTeacher")}
                    </Typography>
                    <HustCopyCodeBlock
                      title={`${t("blocks")} ${block.seq}`}
                      text={block.code}
                      language={mapLanguageToCodeBlockLanguage(selectedLanguage)}
                      showLineNumbers
                      isStudentBlock={block.forStudent}
                    />
                  </Box>
                ))
              ) : (
                <Typography>{t("noBlockCodes")}</Typography>
              )
            ) : (
              <HustCopyCodeBlock
                text={getCombinedBlockCode()}
                language={mapLanguageToCodeBlockLanguage(selectedLanguage)}
                showLineNumbers
              />
            )}
          </Collapse>
        </Box>
      )}

      <Box sx={{ marginTop: "28px" }} />
      <HustCopyCodeBlock
        title={t("solutionSourceCode")}
        language={mapLanguageToCodeBlockLanguage(problemDetail.correctSolutionLanguage)}
        text={problemDetail.correctSolutionSourceCode}
        showLineNumbers
      />

      {problemDetail.isPreloadCode && (
        <Box sx={{ marginTop: "12px" }}>
          <HustCopyCodeBlock
            title={t("preloadCode")}
            text={problemDetail.preloadCode}
            showLineNumbers
          />
        </Box>
      )}

      {problemDetail.isCustomEvaluated && (
        <Box sx={{ marginTop: "24px" }}>
          <HustCopyCodeBlock
            title={t("checkerSourceCode")}
            language={mapLanguageToCodeBlockLanguage(problemDetail.solutionCheckerSourceLanguage)}
            text={problemDetail.solutionCheckerSourceCode}
            showLineNumbers
          />
        </Box>
      )}

      <ListTestCase mode={2} />

      <Box sx={{ height: "36px" }}></Box>
      <ContestsUsingAProblem problemId={problemId} />
    </ProgrammingContestLayout>
  );
}

const screenName = "SCR_MANAGER_PROBLEM_DETAIL";
export default withScreenSecurity(ManagerViewProblemDetailV2, screenName, true);