import {Collapse, Divider, IconButton, Link, Paper, Stack, Tooltip, Typography} from "@mui/material";
import Box from "@mui/material/Box";
import {request} from "api";
import {useEffect, useState} from "react";
import {useParams} from "react-router-dom";
import displayTime from "utils/DateTimeUtils";
import {localeOption} from "utils/NumberFormat";
import {detail, resolveLanguage,} from "./ContestProblemSubmissionDetailViewedByManager";
import ParticipantProgramSubmissionDetailTestCaseByTestCase
  from "./ParticipantProgramSubmissionDetailTestCaseByTestCase";
import {getStatusColor} from "./lib";
import {useTranslation} from "react-i18next";
import {mapLanguageToCodeBlockLanguage, mapLanguageToDisplayName} from "./Constant";
import {makeStyles} from "@material-ui/core/styles";
import ArrowDropDownIcon from '@mui/icons-material/ArrowDropDown';
import FormatListBulletedRoundedIcon from '@mui/icons-material/FormatListBulletedRounded';
import ArticleRoundedIcon from '@mui/icons-material/ArticleRounded';
import HustCopyCodeBlock from "component/common/HustCopyCodeBlock";
import {errorNoti} from "utils/notification";
import {AntTab, AntTabs} from "component/tab";
import {grey} from "@mui/material/colors";
import {dracula, github} from 'react-code-blocks';
import RotatingIconButton from "../../common/RotatingIconButton";

const useStyles = makeStyles((theme) => ({
  expandIcon: {
    transition: theme.transitions.create('transform', {
      duration: theme.transitions.duration.shortest,
    }),
  },
  expandIconOpen: {
    transform: 'rotate(180deg)',
  },
}));

export default function ContestProblemSubmissionDetail() {
  const classes = useStyles();
  const {problemSubmissionId} = useParams();
  const {t} = useTranslation(["education/programmingcontest/testcase", "education/programmingcontest/problem", "education/programmingcontest/contest", 'common']);

  const [submission, setSubmission] = useState({});
  const [comments, setComments] = useState([]);
  const [isSourceCodeExpanded, setIsSourceCodeExpanded] = useState(false);
  const [blockDisplayMode, setBlockDisplayMode] = useState("individual");
  const [selectedLanguage, setSelectedLanguage] = useState(null);
  const [rotationCount, setRotationCount] = useState(0);


  useEffect(() => {
    request(
      "get",
      "/student/submissions/" + problemSubmissionId + "/general-info",
      (res) => {
        setSubmission(res.data);
      },
      {
        onError: (e) => {
          errorNoti(t("common:error"))
        }
      },
    );

    request("GET",
      `submissions/${problemSubmissionId}/comments`,
      (res) => {
        setComments(res.data);
      });
  }, [problemSubmissionId]);

  const handleTabChange = (event, newValue) => {
    setSelectedLanguage(newValue);
  };

  const getCombinedBlockCode = () => {
    if (!submission.blockCodes || !selectedLanguage) return "";
    return submission.blockCodes
      .filter(block => block.language === selectedLanguage)
      .sort((a, b) => a.seq - b.seq)
      .map(block => block.code)
      .join("\n");
  };

  // Initialize selectedLanguage when blockCodes are available
  useEffect(() => {
    if (submission.blockCodes && submission.blockCodes.length > 0) {
      const uniqueLanguages = [...new Set(submission.blockCodes.map(block => block.language))];
      setSelectedLanguage(uniqueLanguages[0]);
    }
  }, [submission.blockCodes]);

  return (
    <Stack sx={{minWidth: 400, flexDirection: {xs: 'column', md: 'row'}, gap: {xs: 2, md: 0}}}>
      <Stack
        sx={{
          display: "flex",
          flexGrow: 1,
          boxShadow: 1,
          overflowY: "auto",
          borderRadius: {xs: 4, md: "16px 0 0 16px"},
          backgroundColor: "#fff",
          height: {md: "calc(100vh - 112px)"},
          order: {xs: 1, md: 0}
        }}
      >
        <Paper
          elevation={0}
          sx={{
            p: 2,
            backgroundColor: "transparent",
          }}
        >
          {(submission.status && submission.status !== "In Progress")
            && (submission.message && !['Evaluated', 'Evaluating', 'Successful'].includes(submission.message))
            && (<Box sx={{mb: 4}}>
                <HustCopyCodeBlock
                  title={t('common:message')}
                  text={submission.message}
                  language="bash"
                />
              </Box>
            )}
          {submission.status
            && !["Compile Error", "In Progress", "N/E Forbidden Ins."].includes(submission.status)
            && (
              <Box sx={{mb: 4}}>
                <ParticipantProgramSubmissionDetailTestCaseByTestCase
                  submissionId={problemSubmissionId}
                />
              </Box>
            )}
          <Box sx={{ mb: 4 }}>
            <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
              <Typography variant="h6">{t("common:sourceCode")}</Typography>
              <RotatingIconButton
                onClick={() => {
                  setRotationCount(rotationCount + 1);
                  setIsSourceCodeExpanded(!isSourceCodeExpanded);
                }}
                aria-expanded={isSourceCodeExpanded}
                aria-label={t("common:sourceCode")}
                color="primary"
                size="small"
                rotation={rotationCount * 180}
                sx={{ml: 1}}
              >
                <ArrowDropDownIcon />
              </RotatingIconButton>
              {submission.blockCodes && submission.blockCodes.length > 0 && isSourceCodeExpanded && (
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
            <Collapse in={isSourceCodeExpanded}>
              {/* 
                TODO: Currently API only returns combined sourceCode, not individual blockCodes
                When API supports returning blockCodes, will display 2 layouts: individual and combined
              */}
              {submission.blockCodes && submission.blockCodes.length > 0 ? (
                <>
                  <AntTabs value={selectedLanguage} onChange={handleTabChange} sx={{marginBottom: "12px"}}>
                    {[...new Set(submission.blockCodes.map(block => block.language))].map((lang) => (
                      <AntTab key={lang} label={mapLanguageToDisplayName(lang)} value={lang} sx={{textTransform: 'none'}}/>
                    ))}
                  </AntTabs>
                  {blockDisplayMode === "individual" ? (
                    submission.blockCodes
                      .filter(block => block.language === selectedLanguage)
                      .sort((a, b) => a.seq - b.seq)
                      .length > 0 ? (
                      submission.blockCodes
                        .filter(block => block.language === selectedLanguage)
                        .sort((a, b) => a.seq - b.seq)
                                                .map((block, index) => (
                          <Box
                            key={block.id || index}
                            sx={{
                              display: 'flex',
                              alignItems: 'flex-start',
                              gap: 2,
                              mb: index === submission.blockCodes.filter(b => b.language === selectedLanguage).length - 1 ? 0 : 1,
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
                        <Typography>{t("common:noBlockCodes")}</Typography>
                      )
                  ) : (
                    <HustCopyCodeBlock
                      text={getCombinedBlockCode()}
                      language={mapLanguageToCodeBlockLanguage(selectedLanguage)}
                      showLineNumbers
                    />
                  )}
                </>
              ) : (
                <HustCopyCodeBlock
                  text={submission.sourceCode}
                  language={resolveLanguage(submission.sourceCodeLanguage)}
                  showLineNumbers
                />
              )}
            </Collapse>
          </Box>
          {comments?.length > 0 && (<Box>
            <Typography variant="h6" sx={{mb: 1}}>
              {t('common:comment')}
            </Typography>
            {comments.map((comment) => (
              <Typography key={comment.id} variant="body2" sx={{mb: 1}}>
                <strong>{comment.username}:</strong> {comment.comment}
              </Typography>
            ))}
          </Box>)}
        </Paper>
      </Stack>
      <Box sx={{order: {xs: 0, md: 1}}}>
        <Paper
          elevation={1}
          sx={{
            p: 2,
            width: {md: 300},
            overflowY: "auto",
            borderRadius: {xs: 4, md: "0 16px 16px 0"},
            height: {md: "calc(100vh - 112px)"},
          }}
        >
          <Typography variant="subtitle1" sx={{fontWeight: 600}}>
            {t('common:submissionDetails')}
          </Typography>
          <Divider sx={{mb: 1}}/>
          <Typography variant="subtitle2" sx={{fontWeight: 600}}>
            {t("common:status")}
          </Typography>
          <Typography
            variant="subtitle2"
            gutterBottom
            sx={{
              color: getStatusColor(`${submission.status}`),
              mb: 2,
              fontWeight: 400,
            }}
          >
            {submission.status}
          </Typography>
          {[
            [
              t("education/programmingcontest/testcase:pass"),
              submission.testCasePass
                ? `${submission.testCasePass} test case`
                : "",
            ],
            [
              t("education/programmingcontest/testcase:point"),
              `${
                submission.point
                  ? submission.point.toLocaleString("fr-FR", localeOption)
                  : 0
              }`,
            ],
            [t("common:language"), mapLanguageToDisplayName(submission.sourceCodeLanguage) || ''],
            [
              t("education/programmingcontest/testcase:totalRuntime"),
              `${
                submission.runtime
                  ? (submission.runtime / 1000).toLocaleString("fr-FR", localeOption)
                  : 0
              } (s)`,
            ],
            [t("common:createdBy"), submission.submittedByUserId],
            [t("common:createdTime"), displayTime(submission.createdAt)],
            // [t("common:lastModified"), displayTime(submission.updateAt)],
            [
              t("education/programmingcontest/problem:problem"),
              <Link
                href={`/programming-contest/student-view-contest-problem-detail/${submission.contestId}/${submission.problemId}`}
                variant="subtitle2"
                underline="none"
                target="_blank"
              >
                {submission.problemId}
              </Link>,
            ],
            [
              t("education/programmingcontest/contest:contest"),
              <Link
                href={`/programming-contest/student-view-contest-detail/${submission.contestId}`}
                variant="subtitle2"
                underline="none"
                target="_blank"
              >
                {submission.contestId}
              </Link>,
            ],
          ].map(([key, value]) => detail(key, value))}
        </Paper>
      </Box>
    </Stack>
  );
}