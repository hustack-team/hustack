import React, { useEffect, useRef, useState } from "react";
import { errorNoti, infoNoti, successNoti } from "../../../../utils/notification";
import GenerateQuizTestGroupDialog from "./GenerateQuizTestGroupDialog";
import { isFunction, request } from "../../../../api";
import { Button, Card, CardContent } from "@mui/material";
import StandardTable from "../../../table/StandardTable";
import QuizTestGroupQuestionList from "../QuizTestGroupQuestionList";
import { toast } from "react-toastify";
import { pdf } from "@react-pdf/renderer";
import ExamQuestionsOfParticipantPDFDocument from "../template/ExamQuestionsOfParticipantPDFDocument";
import FileSaver from "file-saver";
import { makeStyles } from "@material-ui/core/styles";
import CustomizedDialogs from "../../../dialog/CustomizedDialogs";
import TertiaryButton from "../../../button/TertiaryButton";
import PrimaryButton from "../../../button/PrimaryButton";

const useStyles = makeStyles((theme) => ({
  tableWrapper: {
    "& [class^=MTableToolbar-actions]>div>div>span>button": {
      padding: "unset",
      paddingLeft: "8px",
    },
  },
  dialogContent: {
    minWidth: 480,
    minHeight: 64,
  },
  btn: { margin: "4px 8px" },
}));

async function generatePdfDocument(documentData, fileName, onCompleted) {
  try {
    // Generate the PDF blob
    const blob = await pdf(<ExamQuestionsOfParticipantPDFDocument data={documentData} />).toBlob();
    
    // Save the PDF
    FileSaver.saveAs(blob, fileName);

    if (isFunction(onCompleted)) {
      onCompleted();
    }
  } catch (error) {
    console.error("Error generating PDF:", error);
    throw error; // Let the caller handle the error
  }
}

export default function QuizGroupList(props) {
  const classes = useStyles();
  const testId = props.testId;
  const toastId = useRef(null);
  const [quizGroups, setQuizGroups] = useState([]);
  const [studentQuestions, setStudentQuestions] = useState();
  const [quizGroupIdsToDelete, setQuizGroupIdsToDelete] = useState([]);
  const [generateQuizGroupDlgOpen, setGenerateQuizGroupDlgOpen] = useState(false);
  const [deleteConfirmDlgOpen, setDeleteConfirmDlgOpen] = useState(false);
  const [groupIdsToConfirmDelete, setGroupIdsToConfirmDelete] = useState([]);

  useEffect(() => {
    // Fetch both quiz groups and participant questions when the page loads
    Promise.all([getQuizGroups(), getQuestionsOfParticipants()])
      .catch((error) => {
        console.error("Error fetching initial data:", error);
        errorNoti("Đã xảy ra lỗi trong khi tải dữ liệu", 3000);
      });

    return () => {
      toast.dismiss(toastId.current);
    };
  }, []);

  function getQuizGroups() {
    return request(
      "GET",
      `/get-test-groups-info?testId=${testId}`,
      (res) => setQuizGroups(res.data),
      {
        onError: () => errorNoti("Đã xảy ra lỗi trong khi tải dữ liệu", 3000),
      }
    );
  }

  async function getQuestionsOfParticipants() {
    let questionsOfParticipants;
    const successHandler = (res) => {
      questionsOfParticipants = res.data.map((item) => {
        console.log("API Data:", item);
        return {
          ...item,
          testName: item.testName || "Trắc nghiệm DSA",
          courseName: item.courseName || "Cấu trúc dữ liệu và thuật toán",
          scheduleDatetime: item.scheduleDatetime || "N/A",
          duration: item.duration ? Math.round(item.duration / 60) : "N/A",
          userDetail: {
            id: item.userDetail?.id || "Không có MSSV",
            fullName: item.userDetail?.fullName || "Không có tên",
          },
          listQuestion: item.listQuestion?.map((q) => ({
            ...q,
            statement: q.statement || "Câu hỏi không xác định",
            quizChoiceAnswerList: q.quizChoiceAnswerList?.map((ans) => ({
              ...ans,
              choiceAnswerContent: ans.choiceAnswerContent || "Không có lựa chọn",
            })) || [],
            attachment: q.attachment || [],
          })) || [],
        };
      });
      setStudentQuestions(questionsOfParticipants);
    };
    const errorHandlers = {
      onError: () => errorNoti("Đã xảy ra lỗi trong khi tải dữ liệu", true),
    };

    await request(
      "GET",
      `/get-all-quiz-test-group-with-questions-detail/${testId}`,
      successHandler,
      errorHandlers
    );
    return questionsOfParticipants;
  }

  function updateQuizGroupIdsToDelete(newSelectedGroups) {
    setQuizGroupIdsToDelete(newSelectedGroups.map((quizGroup) => quizGroup.quizGroupId));
  }

  function confirmDeleteQuizGroups(deletedQuizGroupIds) {
    if (!deletedQuizGroupIds || deletedQuizGroupIds.length === 0) return;
    
    setGroupIdsToConfirmDelete(deletedQuizGroupIds);
    setDeleteConfirmDlgOpen(true);
  }

  function deleteQuizGroups() {
    const deletedQuizGroupIds = groupIdsToConfirmDelete;
    if (!deletedQuizGroupIds || deletedQuizGroupIds.length === 0) return;

    const formData = new FormData();
    formData.append("testId", testId);
    formData.append("quizTestGroupList", deletedQuizGroupIds.join(";"));

    const refreshQuizGroups = (res) => {
      if (res.data < 0) return;
      const remainingQuizGroups = quizGroups.filter(
        (el) => !deletedQuizGroupIds.includes(el.quizGroupId)
      );
      setQuizGroups(remainingQuizGroups);
    };
    const successHandler = (res) => {
      refreshQuizGroups(res);
      successNoti("Xóa đề thi thành công, xem kết quả trên giao diện!", 3000);
    };
    const errorHandlers = {
      onError: () => errorNoti("Đã xảy ra lỗi khi xóa đề thi", 3000),
    };
    request("POST", "/delete-quiz-test-groups", successHandler, errorHandlers, formData);
    
    // Close the dialog after the request is sent
    setDeleteConfirmDlgOpen(false);
  }

  async function exportExamQuestionsOfAllStudents() {
    try {
      toastId.current = infoNoti("Hệ thống đang chuẩn bị tệp PDF ...");

      if (!studentQuestions || studentQuestions.length === 0) {
        toast.dismiss(toastId.current);
        errorNoti("Không có dữ liệu để xuất PDF! Vui lòng chờ.", 3000);
        return;
      }

      console.log("Tổng số học sinh có câu hỏi:", studentQuestions.length);
      console.log("Chi tiết tất cả câu hỏi của các học sinh:", studentQuestions);

      const data = studentQuestions.map((quizGroupTestDetailModel) => {
        return {
          userDetail: {
            id: quizGroupTestDetailModel.userDetail?.id || "Không có MSSV",
            fullName: quizGroupTestDetailModel.userDetail?.fullName || "Không có tên",
          },
          testName: quizGroupTestDetailModel.testName || "Trắc nghiệm DSA",
          scheduleDatetime: quizGroupTestDetailModel.scheduleDatetime || "N/A",
          courseName: quizGroupTestDetailModel.courseName || "Cấu trúc dữ liệu và thuật toán",
          duration: quizGroupTestDetailModel.duration || "N/A",
          quizGroupId: quizGroupTestDetailModel.quizGroupId || "",
          groupCode: quizGroupTestDetailModel.groupCode || "N/A",
          viewTypeId: quizGroupTestDetailModel.viewTypeId || "",
          listQuestion: quizGroupTestDetailModel.listQuestion?.map((q) => ({
            ...q,
            statement: q.statement || "Câu hỏi không xác định",
            quizChoiceAnswerList: q.quizChoiceAnswerList?.map((ans) => ({
              ...ans,
              choiceAnswerContent: ans.choiceAnswerContent || "Không có lựa chọn",
            })) || [],
            attachment: q.attachment || [],
          })) || [],
        };
      });

      await generatePdfDocument(data, `${testId}.pdf`, () => {
        toast.dismiss(toastId.current);
        successNoti("Tệp PDF đã được tải xuống thành công", 3000);
      });
    } catch (error) {
      console.error("Error exporting PDF:", error);
      toast.dismiss(toastId.current);
      errorNoti("Không thể xuất tệp PDF. Vui lòng thử lại", 3000);
    }
  }

  const columns = [
    { field: "groupCode", title: "Mã đề" },
    { field: "note", title: "Ghi chú" },
    { field: "numStudent", title: "Số sinh viên" },
    { field: "numQuestion", title: "Số câu hỏi" },
    {
      field: "",
      title: "",
      render: (quizGroup) => (
        <DeleteQuizGroupButton deletedGroupIds={[quizGroup.quizGroupId]} variant="outlined" />
      ),
    },
  ];

  const actions = [
    { icon: () => GenerateQuizGroupButton, isFreeAction: true },
    { icon: () => ButtonExportAllDataToPdf, isFreeAction: true },
    {
      icon: () => (
        <DeleteQuizGroupButton deletedGroupIds={quizGroupIdsToDelete} variant="contained" />
      ),
    },
  ];

  const GenerateQuizGroupButton = (
    <Button
      color="primary"
      variant="contained"
      onClick={() => setGenerateQuizGroupDlgOpen(true)}
    >
      Thêm đề
    </Button>
  );

  const ButtonExportAllDataToPdf = (
    <Button
      color="primary"
      variant="contained"
      onClick={() => {
        exportExamQuestionsOfAllStudents();
      }}
    >
      Export PDF
    </Button>
  );

  const DeleteQuizGroupButton = ({ deletedGroupIds, variant }) => (
    <Button
      color="error"
      variant={variant}
      onClick={() => confirmDeleteQuizGroups(deletedGroupIds)}
    >
      Xóa
    </Button>
  );

  const DeleteConfirmDialog = () => (
    <CustomizedDialogs
      open={deleteConfirmDlgOpen}
      handleClose={() => setDeleteConfirmDlgOpen(false)}
      title="Xác nhận xóa"
      content={
        <div className={classes.dialogContent}>
          Bạn có chắc muốn xóa những đề thi này không?
        </div>
      }
      actions={
        <>
          <TertiaryButton className={classes.btn} onClick={() => setDeleteConfirmDlgOpen(false)}>
            Huỷ
          </TertiaryButton>
          <PrimaryButton
            className={classes.btn}
            onClick={deleteQuizGroups}
            color="error"
          >
            Xóa
          </PrimaryButton>
        </>
      }
    />
  );

  return (
    <>
      <Card>
        <CardContent className={classes.tableWrapper}>
          <StandardTable
            title="Danh sách đề thi"
            columns={columns}
            data={quizGroups}
            hideCommandBar
            options={{
              selection: true,
              search: true,
              sorting: true,
            }}
            actions={actions}
            onSelectionChange={updateQuizGroupIdsToDelete}
          />
        </CardContent>
      </Card>

      <QuizTestGroupQuestionList testId={testId} />

      {/* Dialogs */}
      <GenerateQuizTestGroupDialog
        testId={testId}
        onGenerateSuccess={getQuizGroups}
        open={generateQuizGroupDlgOpen}
        onClose={() => setGenerateQuizGroupDlgOpen(false)}
      />

      <DeleteConfirmDialog />
    </>
  );
}