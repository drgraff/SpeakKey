package com.inputstick.api.hid;

import java.util.LinkedList;

public class HIDTransaction {

        private LinkedList<HIDReport> reports;

        public HIDTransaction() {
                reports = new LinkedList<HIDReport>();
        }

        public void addReport(HIDReport report) {
                if (reports != null) {
                        reports.add(report);
                }
        }

        public HIDReport getNextReport() {
                if (reports != null) {
                        if (reports.size() > 0) {
                                return reports.removeFirst();
                        } else {
                                return null;
                        }
                } else {
                        return null;
                }
        }

        public boolean isEmpty() {
                if (reports == null) {
                        return true;
                } else {
                        return reports.isEmpty();
                }
        }

}
