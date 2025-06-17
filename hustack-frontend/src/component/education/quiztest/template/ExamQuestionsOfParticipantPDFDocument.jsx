import {Document, Font, Image, Page, Text, View,} from "@react-pdf/renderer";
import parse from "html-react-parser";
import Footer from "./Footer";
import React from "react";
import fontNormal from "../../../../assets/fonts/IBMPlexSans-Regular.ttf"
import fontBold from "../../../../assets/fonts/IBMPlexSans-Bold.ttf"

const pdfStyles = {
  page: {
    fontFamily: "IBMPlexSans",
    fontSize: 12,
    padding: 40,
    flexGrow: 1,
  },
  question: {
    marginTop: 20,
    marginBottom: 4,
  },
  answer: {
    marginTop: 4,
    marginBottom: 4,
    flexGrow: 1,
    flexShrink: 1,
    display: "inline",
  },
  bold: {
    fontWeight: "bold",
  },
  textLine: {
    marginBottom: 4,
  },
  imageContainer: {
    display: "flex",
    alignItems: "flex-start",
    maxHeight: 300,
  },
  ulChild: {
    paddingLeft: 20,
    marginTop: 4,
    marginBottom: 4,
  },
  blankPage: {
    display: "flex",
    justifyContent: "center",
    alignItems: "center",
    height: "100%",
    color: "#666666",
    fontSize: 14,
  },
};

const checkBoxBase64 =
  "iVBORw0KGgoAAAANSUhEUgAAACQAAAAkCAYAAADhAJiYAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAACoSURBVFhH7dexDcMgFEXRn6zADsxFh0TFFOxAx1zsQE3pBERK632chuKdBhkJ6wpZNn5dX3KQ9xqPwSCEQQiDkOOCVC/GWquUUqS1tmb2GGPEOSfW2jVzT7VD/8QMY+24h4Zqh7z3c8w5z3HXzno+1AiDEAYhDEIYhDAIOS5I9bWPMUrvfV09M85EKaV1dU+1QyGEecOnfgc0Df5KIwxCGIQwCDksSOQD5Zw1Tp9gAfMAAAAASUVORK5CYII=";

function ExamQuestionsOfParticipantPDFDocument({data}) {
  Font.register({
    family: "IBMPlexSans",
    fonts: [
      {
        src: fontNormal,
        fontWeight: 'normal',
      },
      {
        src: fontBold,
        fontWeight: 'bold',
      },
    ],
  });

  return (
    <Document>
      {data?.map(
        (
          {
            userDetail,
            testName,
            scheduleDatetime,
            courseName,
            duration,
            quizGroupId,
            groupCode,
            viewTypeId,
            listQuestion,
          },
          examIndex
        ) => {
          const MainContent = () => (
            <>
              <Page size="A4" style={pdfStyles.page} wrap>
                <View>
                  <Text style={pdfStyles.textLine}>Quiz test: {testName}</Text>
                  <Text style={pdfStyles.textLine}>Code: {groupCode}</Text>
                  <Text style={pdfStyles.textLine}>Học phần: {courseName}</Text>
                  <Text style={pdfStyles.textLine}>MSSV: {userDetail.id}</Text>
                  <Text style={pdfStyles.textLine}>
                    FullName: {userDetail?.fullName}
                  </Text>
                  <Text style={pdfStyles.textLine}>
                    Start Time: {scheduleDatetime}
                  </Text>
                  <Text style={pdfStyles.textLine}>
                    Duration: {duration} minutes
                  </Text>

                  {listQuestion?.map((q, qIndex) => (
                    <View key={q.questionId} style={pdfStyles.question}>
                      {parse(q.statement).map((ele, eIndex) => {
                        if (eIndex > 0) {
                          if (ele.type === "ul") {
                            return (
                              <View
                                key={`ul-${qIndex}-${eIndex}`}
                                style={{
                                  display: "flex",
                                  flexGrow: 1,
                                  flexShrink: 1,
                                }}
                              >
                                {ele.props.children.map((ulChild, ulIndex) => {
                                  if (ulChild.type === "li")
                                    return (
                                      <Text key={`li-${ulIndex}`} style={pdfStyles.ulChild}>
                                        • {ulChild.props.children}
                                      </Text>
                                    );
                                  else if (ulChild !== "\n")
                                    return (
                                      <Text key={`ul-text-${ulIndex}`} style={pdfStyles.ulChild}>
                                        {ulChild}
                                      </Text>
                                    );
                                  return null;
                                })}
                              </View>
                            );
                          } else if (ele !== "\n") {
                            return <Text key={`text-${eIndex}`}>{ele}</Text>;
                          }
                          return null;
                        }
                        return (
                          <Text key={`question-${qIndex}`}>
                            <Text style={pdfStyles.bold}>Question {qIndex + 1}. </Text>
                            {ele}
                          </Text>
                        );
                      })}

                      {q.attachment?.length > 0 &&
                        q.attachment.map((imageBase64, index) => (
                          <View key={`image-${index}`} style={pdfStyles.imageContainer}>
                            <Image
                              src={`data:application/pdf;base64,${imageBase64}`}
                              style={{
                                objectFit: "scale-down",
                              }}
                            />
                          </View>
                        ))}

                      {q.quizChoiceAnswerList.map((ans) => (
                        <View
                          key={ans.choiceAnswerId}
                          style={{
                            display: "flex",
                            flexDirection: "row",
                            alignItems: "center",
                            justifyContent: "center",
                          }}
                        >
                          <Image
                            src={`data:application/pdf;base64,${checkBoxBase64}`}
                            style={{
                              width: "24px",
                              height: "24px",
                            }}
                          />
                          <Text style={pdfStyles.answer}>
                            {parse(ans.choiceAnswerContent)}
                          </Text>
                        </View>
                      ))}
                    </View>
                  ))}
                </View>
                <Footer/>
                <PageTracker examIndex={examIndex}/>
              </Page>
            </>
          );

          const PageTracker = ({examIndex}) => (
            <Text
              render={({pageNumber, totalPages}) => {
                const mainPages = totalPages || 1;
                window[`exam_${examIndex}_pages`] = mainPages;
                const mainBlankPages = (4 - (mainPages % 4)) % 4;
                window[`exam_${examIndex}_main_blank_pages`] = mainBlankPages;
                // console.log(`Exam ${examIndex + 1}: Main content pages`, {
                //   totalPages: mainPages,
                //   pageNumber,
                //   mainBlankPages,
                //   paddedMainPages: mainPages + mainBlankPages - 1,
                // });
                return null;
              }}
            />
          );

          const BlankPages = ({examIndex}) => {
            const mainPages = window[`exam_${examIndex}_pages`] || 1;
            const mainBlankPages = window[`exam_${examIndex}_main_blank_pages`] || 0;
            const totalPages = mainPages + mainBlankPages;
            const extraPages = (4 - (totalPages % 4)) % 4;
            const finalPageCount = totalPages + extraPages;

            // console.log(`Exam ${examIndex + 1}: Blank page calculation`, {
            //   mainPages,
            //   mainBlankPages,
            //   totalPages,
            //   extraPages,
            //   finalPageCount,
            // });

            if (mainBlankPages + extraPages % 4 !== 0) {

            }

            return Array.from({length: (mainBlankPages + extraPages) / 4 * 4}, (_, i) => (
              <Page
                key={`blank-${examIndex}-${i}`}
                size="A4"
                style={pdfStyles.page}
              >
                {/*<View style={pdfStyles.blankPage}>*/}
                {/*  <Text>*/}
                {/*    This page is intentionally left blank*/}
                {/*    {i < mainBlankPages ? " (main content padding)" : ""}*/}
                {/*  </Text>*/}
                {/*</View>*/}
                <Footer/>
              </Page>
            ));
          };

          return (
            <React.Fragment key={`exam-${examIndex}`}>
              <MainContent/>
              <BlankPages examIndex={examIndex}/>
              {/*{console.log(*/}
              {/*  `Exam ${examIndex + 1}: Final page count`,*/}
              {/*  (window[`exam_${examIndex}_pages`] || 1) +*/}
              {/*  (window[`exam_${examIndex}_main_blank_pages`] || 0) +*/}
              {/*  ((4 - (((window[`exam_${examIndex}_pages`] || 1) +*/}
              {/*    (window[`exam_${examIndex}_main_blank_pages`] || 0)) % 4)) % 4)*/}
              {/*)}*/}
            </React.Fragment>
          );
        }
      )}
    </Document>
  );
}

export default ExamQuestionsOfParticipantPDFDocument;