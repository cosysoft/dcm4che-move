package org.cosysoft.dcm.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

public class RawStudy {

	public String patientID;
	public String modalitiesInStudy;
	public String studyID;
	public String studyInstanceUID;
	public String seriesInstanceUID;
	public String sopInstanceUID;
	public String seriesId;

	public int seriesCount;

	public int imageCount;

	@Override
	public String toString() {
		return "RawStudy [patientID=" + patientID + ", studyID=" + studyID
				+ ", studyInstanceUID=" + studyInstanceUID
				+ ", seriesInstanceUID=" + seriesInstanceUID
				+ ", sOPInstanceUID=" + sopInstanceUID + ", seriesId="
				+ seriesId + ", seriesCount=" + seriesCount + ", imageCount="
				+ imageCount + "]";
	}

	/**
	 * 列表转换为树
	 * 
	 * @param rs
	 * @return
	 */
	public static List<Study> toStudy(List<RawStudy> rs) {

		List<Study> studies = new LinkedList<>();
		Multimap<String, RawStudy> m = LinkedHashMultimap.create();

		List<Series> series = null;
		Multimap<String, RawStudy> seriesMap = LinkedHashMultimap.create();
		List<Image> images = null;
		for (RawStudy s : rs) {
			m.put(s.studyID, s);
		}

		for (String key : m.keySet()) {
			Collection<RawStudy> ss = m.get(key);
			if (ss != null && ss.size() > 0) {

				Study s = header(ss).toStudy();
				studies.add(s);

				seriesMap.clear();
				Iterator<RawStudy> iterator = ss.iterator();
				while (iterator.hasNext()) {
					RawStudy i = iterator.next();
					seriesMap.put(i.seriesInstanceUID, i);
				}
				series = new ArrayList<>();
				for (String key2 : seriesMap.keySet()) {
					Collection<RawStudy> ss2 = seriesMap.get(key2);
					if (ss2 != null && ss2.size() > 0) {
						Series s2 = header(ss2).toSeries();
						series.add(s2);

						images = new ArrayList<>();
						for (RawStudy s3 : ss2) {
							images.add(s3.toImage());
						}
						s2.images = images;
					}

				}
				s.series = series;

			}
		}

		return studies;

	}

	public Study toStudy() {
		Study s = new Study();
		s.patientID = this.patientID;
		s.studyID = this.studyID;
		s.studyInstanceUID = this.studyInstanceUID;
		s.seriesCount = this.seriesCount;
		s.modalitiesInStudy = this.modalitiesInStudy;

		return s;
	}

	public Series toSeries() {
		Series s = new Series();
		s.studyInstanceUID = this.studyInstanceUID;
		s.imageCount = this.imageCount;
		s.seriesInstanceUID = this.seriesInstanceUID;

		return s;
	}

	public Image toImage() {
		Image i = new Image();
		i.studyInstanceUID = this.studyInstanceUID;
		i.seriesInstanceUID = this.seriesInstanceUID;
		i.sopInstanceUID = this.sopInstanceUID;
		return i;
	}

	private static <E> E header(Collection<E> c) {
		Iterator<E> iterator = c.iterator();
		while (iterator.hasNext()) {
			return iterator.next();
		}
		return null;
	}

}
