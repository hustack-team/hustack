import EditIcon from "@mui/icons-material/Edit";
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import ShareIcon from '@mui/icons-material/Share';

import {Box, Collapse, Grid, IconButton, LinearProgress, Stack, TextField, Tooltip, Typography,} from "@mui/material";
import {request, saveFile} from "api";
import withScreenSecurity from "component/withScreenSecurity";
import {useEffect, useState} from "react";
import "react-draft-wysiwyg/dist/react-draft-wysiwyg.css";
import {useTranslation} from "react-i18next";
import {useHistory, useParams} from "react-router-dom";
import FileUploadZone from "utils/FileUpload/FileUploadZone";
import {PROBLEM_ROLE, PROBLEM_STATUS} from "utils/constants";
import RichTextEditor from "../../common/editor/RichTextEditor";
import {
  COMPUTER_LANGUAGES,
  CUSTOM_EVALUATION,
  mapLanguageToCodeBlockLanguage,
  mapLanguageToDisplayName
} from "./Constant";
import ContestsUsingAProblem from "./ContestsUsingAProblem";
import ListTestCase from "./ListTestCase";
import {localeOption} from "utils/NumberFormat";
import {detail} from "./ContestProblemSubmissionDetailViewedByManager";
import ProgrammingContestLayout from "./ProgrammingContestLayout";
import PrimaryButton from "../../button/PrimaryButton";
import TertiaryButton from "../../button/TertiaryButton";
import {AntTab, AntTabs} from "component/tab";
import ArrowDropDownIcon from '@mui/icons-material/ArrowDropDown';
import FormatListBulletedRoundedIcon from '@mui/icons-material/FormatListBulletedRounded';
import ArticleRoundedIcon from '@mui/icons-material/ArticleRounded';
import {dracula, github} from 'react-code-blocks';
import {grey} from '@mui/material/colors';
import RotatingIconButton from "../../common/RotatingIconButton";
import {errorNoti} from "utils/notification";
import {getLevels, getStatuses, PROGRAMMING_LANGUAGES} from "./CreateProblem";
import HustCopyCodeBlock from "../../common/HustCopyCodeBlock";
import CustomizedDialogs from "../../dialog/CustomizedDialogs";
import {makeStyles} from "@material-ui/core/styles";
import {useForm} from "react-hook-form";

const useStyles = makeStyles((theme) => ({
  dialogContent: {
    minWidth: 500
  },
}));

function ManagerViewProblemDetailV2() {
  const classes = useStyles();
  const {problemId} = useParams();
  const history = useHistory();

  const {t} = useTranslation([
    "education/programmingcontest/problem",
    "common",
    "validation",
  ]);

  const [fetchedImageArray, setFetchedImageArray] = useState([]);
  const [openCloneDialog, setOpenCloneDialog] = useState(false);
  const [loading, setLoading] = useState(true);

  const {
    register,
    handleSubmit,
    reset,
    watch,
    errors
  } = useForm({
    defaultValues: {
      problemId: "",
      problemName: "",
    },
  });

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
    Object.fromEntries(PROGRAMMING_LANGUAGES.map(({value}) => [value, []]))
  );
  const [selectedLanguage, setSelectedLanguage] = useState(COMPUTER_LANGUAGES.CPP17);
  const [isBlockCodesExpanded, setIsBlockCodesExpanded] = useState(false);
  const [blockDisplayMode, setBlockDisplayMode] = useState("individual");
  const [rotationCount, setRotationCount] = useState(0);

  const handleExit = () => {
    history.push(`/programming-contest/list-problems`);
  };

  useEffect(() => {
    request("get", "teacher/problems/" + problemId, (res) => {
      setLoading(false);
      const data = res.data;

      if (data.attachments && data.attachments.length !== 0) {
        setFetchedImageArray(data.attachments);
      }

      setProblemDetail({
        ...data,
        public: data.publicProblem,
        solutionCheckerSourceCode: data.solutionCheckerSourceCode || "",
        isCustomEvaluated: data.scoreEvaluationType === CUSTOM_EVALUATION,
        description: data.problemDescription,
        categoryId: data.categoryId || 0,
        roles: data.roles || [],
      });

      if (data.categoryId === 1) {
        const newBlockCodes = Object.fromEntries(
          PROGRAMMING_LANGUAGES.map(({value}) => [value, []])
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

        // Sort blocks by seq for each language
        Object.keys(newBlockCodes).forEach((lang) => {
          newBlockCodes[lang].sort((a, b) => a.seq - b.seq);
        });
        setBlockCodes(newBlockCodes);

        // Select the first language with block codes
        const firstLanguageWithBlocks = PROGRAMMING_LANGUAGES.find(lang =>
          newBlockCodes[lang.value] && newBlockCodes[lang.value].length > 0
        );
        if (firstLanguageWithBlocks) {
          setSelectedLanguage(firstLanguageWithBlocks.value);
        }
      }
    }, {
      onError: (e) => {
        setLoading(false);
        if (e?.response?.status === 403) {
          history.push("/programming-contest/list-problems");
        } else {
          errorNoti(t("common:error"), 3000);
        }
      },
    });
  }, [problemId]);

  const hasSpecialCharacterProblemId = (value) => {
    return !new RegExp(/^[0-9a-zA-Z_-]*$/).test(value);
  };

  const hasSpecialCharacterProblemName = (value) => {
    return !new RegExp(/^[0-9a-zA-Z ]*$/).test(value);
  };

  const handleCloneDialogOpen = () => {
    setOpenCloneDialog(true);
  };

  const handleCloneDialogClose = () => {
    setOpenCloneDialog(false);
    reset();
  };

  const onSubmit = (data) => {
    const body = {
      oldProblemId: problemId,
      newProblemId: data.problemId,
      newProblemName: data.problemName,
    };

    request(
      "post",
      "/teachers/problems/clone",
      (res) => {
        handleCloneDialogClose();
        history.push(`/programming-contest/manager-view-problem-detail/${data.problemId}`);
      },
      {
        onError: (error) => {
          if (error?.response?.status === 403) {
            history.push("/programming-contest/list-problems");
          } else if (error?.response?.status !== 400 &&
            error?.response?.status !== 404 &&
            error?.response?.status !== 409) {
            errorNoti(t("common:error"), 3000);
          }
        },
        400: (error) => {
          errorNoti(t("common:invalidInput"), 3000);
        },
        404: (error) => {
          errorNoti(t("common:problemNotFound"), 3000);
        },
        409: (error) => {
          errorNoti(t("common:problemAlreadyExists"), 3000);
        },
      },
      body
    );
  };

  const handleTabChange = (event, newValue) => {
    setSelectedLanguage(newValue);
  };

  const getCombinedBlockCode = () => {
    const blocks = blockCodes[selectedLanguage] || [];
    return blocks.map((block) => block.code).join('\n');
  };

  const handleDownloadFile = (file) => {
    request(
      "GET",
      `/problems/${problemId}/attachments/${file.id}`,
      (res) => {
        const fileName = file.fileName || file.id;
        saveFile(fileName, res.data);
      },
      {
        onError: (error) => {
          if (error?.response?.status === 403) {
            history.push("/programming-contest/list-problems");
          } else {
            errorNoti(t('common:error'), 3000);
          }
        },
      },
      null,
      {responseType: "blob"}
    );
  };

  const canEdit = problemDetail.roles.includes(PROBLEM_ROLE.OWNER) ||
    problemDetail.roles.includes(PROBLEM_ROLE.EDITOR);

  const canClone = (() => {
    const isOpen = problemDetail.status === PROBLEM_STATUS.OPEN;
    const isPublic = problemDetail.public;
    if (isOpen && isPublic) return true;

    const isOwner = problemDetail.roles.includes(PROBLEM_ROLE.OWNER);
    if (isOwner) return true;

    const isEditor = problemDetail.roles.includes(PROBLEM_ROLE.EDITOR);
    const isViewer = problemDetail.roles.includes(PROBLEM_ROLE.VIEWER);
    if (isOpen && (isEditor || isViewer)) return true;

    if (!isOpen && isEditor) return true;

    return false;
  })();

  const canManageRole = problemDetail.roles.includes(PROBLEM_ROLE.OWNER);

  return (
    <ProgrammingContestLayout title={t("viewProblem")} onBack={handleExit}>
      <Stack direction="row" spacing={2} mb={1.5} justifyContent="space-between">
        <Typography variant="h6" component='span'>
          {t("generalInfo")}
        </Typography>

        <Stack direction="row" spacing={2}>
          {canEdit && (
            <PrimaryButton
              onClick={() => {
                history.push("/programming-contest/edit-problem/" + problemId);
              }}
              startIcon={<EditIcon/>}
            >
              {t("common:edit", {name: ''})}
            </PrimaryButton>
          )}
          {canClone && (
            <TertiaryButton
              variant="outlined"
              onClick={handleCloneDialogOpen}
              startIcon={<ContentCopyIcon/>}
            >
              {t("clone")}
            </TertiaryButton>
          )}
          {canManageRole && (
            <TertiaryButton
              variant="outlined"
              onClick={() => {
                history.push(
                  "/programming-contest/user-contest-problem-role-management/" +
                  problemId
                );
              }}
              startIcon={<ShareIcon/>}
            >
              {t("common:share")}
            </TertiaryButton>
          )}
        </Stack>
      </Stack>

      <CustomizedDialogs
        open={openCloneDialog}
        handleClose={handleCloneDialogClose}
        title={t("clone")}
        contentTopDivider
        content={
          <form onSubmit={handleSubmit(onSubmit)}>
            <Stack spacing={2}>
              <TextField
                autoFocus
                label={t("problemId") + " *"}
                name={'problemId'}
                fullWidth
                size='small'
                variant="outlined"
                value={watch("problemId")}
                error={!!errors.problemId}
                helperText={errors.problemId?.message || ""}
                inputRef={register({
                  required: t("validation:required"),
                  maxLength: {
                    value: 100,
                    message: t("validation:maxLength", {max: 100})
                  },
                  validate: (value) => {
                    const testValue = value || "";
                    if (hasSpecialCharacterProblemId(testValue)) {
                      return t("common:invalidCharactersInProblemId");
                    }
                    return true;
                  }
                })}
              />
              <TextField
                label={t("problemName") + " *"}
                name={'problemName'}
                fullWidth
                size='small'
                variant="outlined"
                value={watch("problemName")}
                error={!!errors.problemName}
                helperText={errors.problemName?.message || ""}
                inputRef={register({
                  required: t("validation:required"),
                  maxLength: {
                    value: 100,
                    message: t("validation:maxLength", {max: 100})
                  },
                  validate: (value) => {
                    const testValue = value || "";
                    if (hasSpecialCharacterProblemName(testValue)) {
                      return t("common:invalidCharactersInProblemName");
                    }
                    return true;
                  }
                })}
              />
            </Stack>

            <Stack direction="row" spacing={2} justifyContent="center" sx={{mt: 3}}>
              <TertiaryButton
                color={'inherit'}
                onClick={handleCloneDialogClose}
              >
                {t("common:cancel")}
              </TertiaryButton>
              <PrimaryButton
                type="submit"
              >
                {t("common:clone")}
              </PrimaryButton>
            </Stack>
          </form>
        }
        classNames={{
          content: classes.dialogContent
        }}
      />

      {loading && <LinearProgress/>}
      <Grid container spacing={2} display={loading ? "none" : ""}>
        {[
          [t("problemName"), problemDetail.problemName],
          [t("level"), getLevels(t).find(item => item.value === problemDetail.levelId)?.label],
          [t("status"), getStatuses(t).find(item => item.value === problemDetail.status)?.label],
          [
            t("public", {ns: "common"}),
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

      <Box sx={{marginTop: 3, mb: 2}}>
        <Typography variant="h6" sx={{mb: 1}}>
          {t("common:description")}
        </Typography>
        {problemDetail.description && problemDetail.description.trim() !== '' && (
          <RichTextEditor
            toolbarHidden
            content={problemDetail.description}
            readOnly
            editorStyle={{editor: {}}}
          />
        )}
      </Box>

      {problemDetail.categoryId === 1 && (
        <>
          <Box sx={{display: 'flex', alignItems: 'center', marginTop: 2}}>
            <Typography variant="body1" sx={{ml: 0, fontWeight: 500}}>
              {t("common:blockCode")}
            </Typography>
            <RotatingIconButton
              onClick={() => {
                setRotationCount(rotationCount + 1);
                setIsBlockCodesExpanded(!isBlockCodesExpanded);
              }}
              aria-expanded={isBlockCodesExpanded}
              aria-label={t("common:blockCode")}
              color="primary"
              size="small"
              rotation={rotationCount * 180}
              sx={{ml: 1}}
            >
              <ArrowDropDownIcon/>
            </RotatingIconButton>
            {isBlockCodesExpanded && (
              <Box sx={{display: 'flex', alignItems: 'center', ml: 'auto', gap: 1}}>
                <Tooltip title={t("common:listBlockLayout")}>
                  <IconButton
                    onClick={() => setBlockDisplayMode("individual")}
                    color={blockDisplayMode === "individual" ? "primary" : "default"}
                    size="small"
                  >
                    <FormatListBulletedRoundedIcon/>
                  </IconButton>
                </Tooltip>
                <Tooltip title={t("common:combinedBlockLayout")}>
                  <IconButton
                    onClick={() => setBlockDisplayMode("combined")}
                    color={blockDisplayMode === "combined" ? "primary" : "default"}
                    size="small"
                  >
                    <ArticleRoundedIcon/>
                  </IconButton>
                </Tooltip>
              </Box>
            )}
          </Box>
          <Collapse in={isBlockCodesExpanded}>
            <AntTabs value={selectedLanguage} onChange={handleTabChange} sx={{marginBottom: "12px"}}>
              {PROGRAMMING_LANGUAGES.filter(lang =>
                blockCodes[lang.value] && blockCodes[lang.value].length > 0
              ).map((lang) => (
                <AntTab key={lang.value} label={mapLanguageToDisplayName(lang.value)} value={lang.value}
                        sx={{textTransform: 'none'}}/>
              ))}
            </AntTabs>
            {blockDisplayMode === "individual" ? (
              blockCodes[selectedLanguage].length > 0 ? (
                blockCodes[selectedLanguage].map((block, index) => (
                  <Box
                    key={block.id || index}
                    sx={{
                      display: 'flex',
                      alignItems: 'flex-start',
                      gap: 2,
                      mb: index === blockCodes[selectedLanguage].length - 1 ? 0 : 1,
                    }}
                  >
                    <Box
                      sx={{
                        width: '48px',
                        minWidth: '48px',
                        display: 'flex',
                        justifyContent: 'center',
                        alignItems: 'flex-start',
                        pt: '14px',
                      }}
                    >
                      <Typography
                        variant="body2"
                        sx={{
                          color: 'text.secondary',
                          fontWeight: 500,
                        }}
                      >
                        {block.seq}
                      </Typography>
                    </Box>
                    <Box sx={{flex: 1}}>
                      <Typography
                        sx={{
                          position: 'absolute',
                          top: '8px',
                          right: '8px',
                          fontSize: '0.875rem',
                          color: 'text.secondary',
                        }}
                      >
                        {block.forStudent ? t("common:forStudent") : t("common:forTeacher")}
                      </Typography>
                      <Box sx={block.forStudent ? {border: `1px solid ${grey[900]}`, borderRadius: 1} : {}}>
                        <HustCopyCodeBlock
                          text={block.code}
                          language={mapLanguageToCodeBlockLanguage(selectedLanguage)}
                          showLineNumbers
                          isStudentBlock={block.forStudent}
                          theme={block.forStudent ? github : dracula}
                        />
                      </Box>
                    </Box>
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
        </>
      )}

      {problemDetail.sampleTestCase && (
        <Box sx={{mt: 2}}>
          <Typography variant="body1" sx={{mb: 1, fontWeight: 500}}>
            {t("sampleTestCase")}
          </Typography>
          <HustCopyCodeBlock text={problemDetail.sampleTestCase}/>
        </Box>
      )}

      {fetchedImageArray.length !== 0 && (
        <Box sx={{mt: 2}}>
          <Typography variant="body1" sx={{mb: 1, fontWeight: 500}}>
            {t("common:attachments")}
          </Typography>
          {fetchedImageArray.map((file) => (
            <FileUploadZone key={file.id} file={file} removable={false} onDownload={handleDownloadFile}/>
          ))}
        </Box>
      )}

      <Typography variant="h6" sx={{mb: 1, mt: 3}}>
        {t("solutionSourceCode")}
      </Typography>
      <HustCopyCodeBlock
        language={mapLanguageToCodeBlockLanguage(problemDetail.correctSolutionLanguage)}
        text={problemDetail.correctSolutionSourceCode}
        showLineNumbers
      />

      {problemDetail.isPreloadCode && (
        <Box sx={{marginTop: "12px"}}>
          <Typography variant="h6" sx={{mb: 1}}>
            {t("preloadCode")}
          </Typography>
          <HustCopyCodeBlock
            text={problemDetail.preloadCode}
            showLineNumbers
          />
        </Box>
      )}

      {problemDetail.isCustomEvaluated && (
        <Box sx={{marginTop: "24px"}}>
          <Typography variant="h6" sx={{mb: 1}}>
            {t("checkerSourceCode")}
          </Typography>
          <HustCopyCodeBlock
            language={mapLanguageToCodeBlockLanguage(problemDetail.solutionCheckerSourceLanguage)}
            text={problemDetail.solutionCheckerSourceCode}
            showLineNumbers
          />
        </Box>
      )}

      <Box sx={{mt: 3}}/>
      <ListTestCase mode={2} hideWhenEmpty={true}/>

      <Box sx={{mt: 3}}/>
      <ContestsUsingAProblem problemId={problemId}/>
    </ProgrammingContestLayout>
  );
}

const screenName = "SCR_MANAGER_PROBLEM_DETAIL";
export default withScreenSecurity(ManagerViewProblemDetailV2, screenName, true);