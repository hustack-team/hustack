package com.hust.baseweb.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.HashSet;
import java.util.Random;

public class CommonUtils {

    public static int SEQ_ID_LEN = 6;

    public static String buildSeqId(int idx) {
        StringBuilder stringBuilder = new StringBuilder(idx + "");
        while (stringBuilder.length() < SEQ_ID_LEN) {
            stringBuilder.insert(0, "0");
        }
        return stringBuilder.toString();
    }

    public static int convertStr2Int(String s) {
        try {
            //s.replaceAll("0"," ");
            System.out.println("convertStr2Int, s = " + s);

            s = s.trim();
            int num = Integer.valueOf(s);
            return num;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static String[] generateNextSeqId(String[] seqIds, int sz) {
        String[] res = new String[sz];
        int[] idx = new int[seqIds.length];
        try {
            int minValue = 0;
            HashSet<Integer> S = new HashSet<Integer>();
            for (int i = 0; i < seqIds.length; i++) {

                idx[i] = convertStr2Int(seqIds[i]);
                System.out.println("generateNextSeqId, convert get " + idx[i]);
                if (idx[i] < 0) {
                    return null;
                }
                if (i == 0) {
                    minValue = idx[i];
                } else {
                    if (minValue > idx[i]) {
                        minValue = idx[i];
                    }
                }
                S.add(idx[i]);
            }
            for (int i = 0; i < sz; i++) {
                int n = minValue;
                do {
                    n = n + 1;
                } while (S.contains(n));
                S.add(n);
                res[i] = buildSeqId(n);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return res;
    }

    public static int[] genRandom(int a, int n, Random R) {
        // return a random elements from 0,...,n-1
        if (a > n) {
            return null;
        }
        int[] idx = new int[n];
        for (int j = 0; j < n; j++) {
            idx[j] = j;
        }
        int[] ans = new int[a];
        for (int j = 0; j < a; j++) {
            int k = R.nextInt(n);
            ans[j] = idx[k];
            // remove the kth element by swapping idx[k] with idx[n-1]
            int tmp = idx[k];
            idx[k] = idx[n - 1];
            idx[n - 1] = tmp;
            n = n - 1;
        }
        return ans;
    }

    public static Pageable getPageable(Integer page, Integer size, Sort sort) {
        if (page == null || page < 0) {
            page = 0;
        }

        if (size == null || size < 1) {
            size = Integer.MAX_VALUE;
        }

        return PageRequest.of(page, size, sort);
    }

    public static String getClientIp(HttpServletRequest request) {
        String ip = StringUtils.trimToNull(request.getHeader("X-Real-IP"));
        if (StringUtils.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        ip = StringUtils.trimToNull(request.getHeader("X-Forwarded-For"));
        if (StringUtils.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }
}
