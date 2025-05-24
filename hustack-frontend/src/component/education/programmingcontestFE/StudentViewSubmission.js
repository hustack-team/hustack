import ReplayIcon from "@mui/icons-material/Replay";
import { Box, LinearProgress, Paper, Typography, Checkbox } from "@mui/material";
import { request } from "api";
import HustCopyCodeBlock from "component/common/HustCopyCodeBlock";
import HustModal from "component/common/HustModal";
import StandardTable from "component/table/StandardTable";
import React, { forwardRef, useEffect, useImperativeHandle, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useParams } from "react-router-dom";
import { getStatusColor } from "./lib";
import { mapLanguageToDisplayName } from "./Constant";
import { toFormattedDateTime } from "../../../utils/dateutils";
import { localeOption } from "../../../utils/NumberFormat";
import { errorNoti, successNoti } from "utils/notification";

const StudentViewSubmission = forwardRef((props, ref) => {
  const { t } = useTranslation([
    "education/programmingcontest/studentviewcontestdetail",
    "education/programmingcontest/testcase",
    "common",
  ]);
  const { contestId } = useParams();
  const problemId = props?.problemId || "";
  const [submissions, setSubmissions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedRowData, setSelectedRowData] = useState();
  const [openModalMessage, setOpenModalMessage] = useState(false);
  const [comments, setComments] = useState([]);
  const [loadingComments, setLoadingComments] = useState(false);
  const [currentLockedSubmissionId, setCurrentLockedSubmissionId] = useState(null);
  const [allowPinSubmission, setAllowPinSubmission] = useState(0);

  const getCommentsBySubmissionId = async (submissionId) => {
    setLoadingComments(true);
    const res = await request("get", `/submissions/${submissionId}/comments`);
    setComments(res.data);
    setLoadingComments(false);
  };

  const handleCommentClick = (rowData) => {
    setSelectedRowData(rowData);
    setOpenModalMessage(true);
    getCommentsBySubmissionId(rowData["contestSubmissionId"]);
  };

  const getSubmissions = () => {
    let requestUrl;
    if (problemId !== "") {
      requestUrl = "/contests/users/submissions?contestId=" + contestId + "&problemId=" + problemId;
    } else {
      requestUrl = "/contests/" + contestId + "/users/submissions";
    }

    request(
      "GET",
      requestUrl,
      (res) => {
        const submissionsData = res.data.content;
        setSubmissions(submissionsData);
        const lockedSubmission = submissionsData.find(
          (submission) => submission.finalSelectedSubmission === 1
        );
        setCurrentLockedSubmissionId(
          lockedSubmission ? lockedSubmission.contestSubmissionId : null
        );
        if (submissionsData.length > 0) {
          setAllowPinSubmission(submissionsData[0].allowParticipantPinSubmission);
        }
        setLoading(false);
      }
    );
  };

  const handleSwitchSubmission = (newSubmissionId) => {
    if (newSubmissionId === currentLockedSubmissionId) {
      return; 
    }

    setLoading(true);

    request(
      "post",
      `/contests/users/submissions/lock?contestId=${contestId}&problemId=${problemId}`,
      (res) => {
        successNoti("Submission switched successfully", 3000);
        setCurrentLockedSubmissionId(newSubmissionId);
        getSubmissions();
      },
      {
        onError: (e) => {
          errorNoti("Failed to switch submission", 3000);
          setLoading(false);
        },
      },
      {
        newSubmissionId: newSubmissionId,
        oldSubmissionId: currentLockedSubmissionId,
      }
    );
  };

  useEffect(() => {
    getSubmissions();
  }, []);

  const columns = [
    {
      title: "ID",
      cellStyle: { minWidth: "80px" },
      sorting: false,
      render: (rowData) => (
        <Link
          to={{
            pathname: `/programming-contest/contest-problem-submission-detail/${rowData["contestSubmissionId"]}`,
          }}
        >
          {rowData["contestSubmissionId"].substring(0, 6)}
        </Link>
      ),
    },
    { title: t("problem"), field: "problemId" },
    {
      title: t("submissionList.status"),
      field: "status",
      cellStyle: { minWidth: 120 },
      render: (rowData) => (
        <span style={{ color: getStatusColor(`${rowData.status}`) }}>
          {`${rowData.status}`}
        </span>
      ),
    },
    {
      title: t("education/programmingcontest/testcase:point"),
      field: "point",
      type: "numeric",
      render: (rowData) => rowData.point?.toLocaleString("fr-FR", localeOption),
    },
    {
      title: t("education/programmingcontest/testcase:pass"),
      field: "testCasePass",
      sorting: false,
      cellStyle: { minWidth: 80 },
    },
    ...(allowPinSubmission === 1
      ? [
          {
            title: t("common:finalSubmission"),
            sorting: false,
            cellStyle: { minWidth: 150, textAlign: "center" },
            render: (rowData) => (
              <Checkbox
                checked={rowData.finalSelectedSubmission === 1}
                onChange={() =>
                  handleSwitchSubmission(rowData.contestSubmissionId)
                }
                disabled={loading}
              />
            ),
          },
        ]
      : []),
    {
      title: t("common:language"),
      field: "sourceCodeLanguage",
      cellStyle: { minWidth: 100 },
      render: (rowData) => mapLanguageToDisplayName(rowData.sourceCodeLanguage),
    },
    {
      title: t("common:createdTime"),
      field: "createAt",
      cellStyle: { minWidth: 150 },
      render: (rowData) => toFormattedDateTime(rowData.createAt),
    },
  ];

  const handleRefresh = () => {
    setLoading(true);
    getSubmissions();
  };

  useImperativeHandle(ref, () => ({
    refreshSubmission() {
      handleRefresh();
    },
  }));

  const ModalMessage = ({ rowData }) => {
    let message = "";
    let detailLink = "";

    if (rowData) {
      if (rowData["message"]) message = rowData["message"];
      if (rowData["contestSubmissionId"]) detailLink = rowData["contestSubmissionId"];
    }

    return (
      <HustModal
        open={openModalMessage}
        onClose={() => setOpenModalMessage(false)}
        isNotShowCloseButton
        showCloseBtnTitle={false}
      >
        <HustCopyCodeBlock title="Message" text={message} />
        <Typography variant="h6" sx={{ mt: 2 }}>Comments:</Typography>
        <Box sx={{ maxHeight: "400px", overflowY: "auto" }}>
          {loadingComments && <LinearProgress />}
          {comments.length > 0 ? (
            comments.map((comment) => (
              <Typography key={comment.id} variant="body2" sx={{ mb: 1 }}>
                <strong>{comment.username}:</strong> {comment.comment}
              </Typography>
            ))
          ) : (
            <Typography variant="body2" sx={{ mb: 1 }}>No comments available.</Typography>
          )}
        </Box>
      </HustModal>
    );
  };

  return (
    <>
      <StandardTable
        hideCommandBar
        columns={columns}
        data={submissions}
        loading={loading}
        options={{
          selection: false,
          pageSize: 5,
        }}
        components={{
          Container: (props) => <Paper {...props} elevation={0} />,
        }}
        actions={[
          {
            icon: () => <ReplayIcon />,
            tooltip: t("common:refresh"),
            isFreeAction: true,
            onClick: handleRefresh,
          },
        ]}
      />
      <ModalMessage rowData={selectedRowData} />
    </>
  );
});

export default StudentViewSubmission;