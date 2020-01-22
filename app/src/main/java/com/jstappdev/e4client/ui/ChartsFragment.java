package com.jstappdev.e4client.ui;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.jstappdev.e4client.R;
import com.jstappdev.e4client.SharedViewModel;
import com.jstappdev.e4client.data.E4SessionData;
import com.jstappdev.e4client.util.Utils;
import com.scichart.charting.Direction2D;
import com.scichart.charting.model.dataSeries.XyDataSeries;
import com.scichart.charting.numerics.labelProviders.DateLabelProvider;
import com.scichart.charting.visuals.SciChartSurface;
import com.scichart.charting.visuals.annotations.AnnotationCoordinateMode;
import com.scichart.charting.visuals.annotations.AxisMarkerAnnotation;
import com.scichart.charting.visuals.annotations.HorizontalAnchorPoint;
import com.scichart.charting.visuals.annotations.TextAnnotation;
import com.scichart.charting.visuals.annotations.VerticalAnchorPoint;
import com.scichart.charting.visuals.annotations.VerticalLineAnnotation;
import com.scichart.charting.visuals.axes.AutoRange;
import com.scichart.charting.visuals.axes.AxisAlignment;
import com.scichart.charting.visuals.axes.IAxis;
import com.scichart.charting.visuals.renderableSeries.IRenderableSeries;
import com.scichart.charting.visuals.synchronization.SciChartVerticalGroup;
import com.scichart.data.model.DateRange;
import com.scichart.data.model.DoubleRange;
import com.scichart.drawing.utility.ColorUtil;
import com.scichart.extensions.builders.SciChartBuilder;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ChartsFragment extends Fragment {

    private static final String hrAxisTitle = "HR";
    private static final String edaAxisTitle = "EDA";
    private static final String tempAxisTitle = "Temp";
    private static final String accAxisTitle = "Acc";

    private static final int AXIS_MARKER_COLOR = 0xFFFFA500;
    private final SciChartVerticalGroup verticalGroup = new SciChartVerticalGroup();
    private final DateRange sharedXRange = new DateRange();

    @BindView(R.id.edaChart)
    SciChartSurface edaChart;
    @BindView(R.id.hrChart)
    SciChartSurface hrChart;
    @BindView(R.id.tempChart)
    SciChartSurface tempChart;
    @BindView(R.id.accChart)
    SciChartSurface accChart;

    private SharedViewModel sharedViewModel;
    private SciChartBuilder sciChartBuilder;

    private XyDataSeries<Double, Float> edaLineData;
    private XyDataSeries<Double, Float> cleanedEdaLineData;
    private XyDataSeries<Double, Float> hrLineData;
    private XyDataSeries<Double, Float> averagedHrLineData;
    private XyDataSeries<Double, Float> tempLineData;
    private XyDataSeries<Double, Float> accLineData;

    private AxisMarkerAnnotation tempAxisMarker;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        final View root = inflater.inflate(R.layout.fragment_charts, container, false);

        ButterKnife.bind(this, root);

        SciChartBuilder.init(requireActivity());
        sciChartBuilder = SciChartBuilder.instance();

        edaLineData = sciChartBuilder.newXyDataSeries(Double.class, Float.class).build();
        hrLineData = sciChartBuilder.newXyDataSeries(Double.class, Float.class).build();
        tempLineData = sciChartBuilder.newXyDataSeries(Double.class, Float.class).build();
        accLineData = sciChartBuilder.newXyDataSeries(Double.class, Float.class).build();
        cleanedEdaLineData = sciChartBuilder.newXyDataSeries(Double.class, Float.class).build();
        averagedHrLineData = sciChartBuilder.newXyDataSeries(Double.class, Float.class).build();
        tempAxisMarker = sciChartBuilder.newAxisMarkerAnnotation().withY1(0d).withBackgroundColor(AXIS_MARKER_COLOR).build();

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        setupCharts();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        SciChartBuilder.dispose();
    }

    private void setupChart(final SciChartSurface chartSurface, final String yAxisTitle, final XyDataSeries<Double, Float> lineData, boolean showXAxis) {

        //noinspection ConstantConditions
        if (sharedViewModel.getIsConnected().getValue())
            showXAxis = false;

        final IAxis xAxis = sciChartBuilder.newDateAxis()
                .withVisibleRange(sharedXRange)
                .withDrawMinorGridLines(false)
                .withGrowBy(0, 0.1)
                .withVisibility(showXAxis ? View.VISIBLE : View.GONE)
                .withAxisAlignment(AxisAlignment.Top)
                .build();
        xAxis.setLabelProvider(new DateLabelProviderEx());

        // Create a numeric Y axis
        final IAxis yAxis = sciChartBuilder.newNumericAxis()
                .withAutoRangeMode(AutoRange.Always)
                .withAxisTitle(yAxisTitle)
                .withDrawMinorGridLines(true)
                .withDrawMajorGridLines(true)
                .withGrowBy(new DoubleRange(0d, 0d))
                .build();

        // Add the Y axis to the YAxes collection of the surface
        Collections.addAll(chartSurface.getYAxes(), yAxis);
        // Add the X axis to the XAxes collection of the surface
        Collections.addAll(chartSurface.getXAxes(), xAxis);

        // Create and configure a line series
        final IRenderableSeries lineSeries = sciChartBuilder.newLineSeries()
                .withDataSeries(lineData)
                .withStrokeStyle(ColorUtil.DarkGreen, 2f, false)
                .build();

        chartSurface.getRenderableSeries().add(lineSeries);

        if (!sharedViewModel.getIsConnected().getValue())
            Collections.addAll(chartSurface.getChartModifiers(), sciChartBuilder.newModifierGroup()
                    .withXAxisDragModifier().build()
                    .withZoomPanModifier().withReceiveHandledEvents(true).withXyDirection(Direction2D.XDirection).build()
                    .withZoomExtentsModifier().build()
                    .build());
    }

    private void setupCharts() {
        final LifecycleOwner owner = getViewLifecycleOwner();

        setupChart(edaChart, edaAxisTitle, edaLineData, false);
        setupChart(hrChart, hrAxisTitle, hrLineData, false);
        setupChart(tempChart, tempAxisTitle, tempLineData, true);
        setupChart(accChart, accAxisTitle, accLineData, false);

        verticalGroup.addSurfaceToGroup(edaChart);
        verticalGroup.addSurfaceToGroup(hrChart);
        verticalGroup.addSurfaceToGroup(tempChart);
        verticalGroup.addSurfaceToGroup(accChart);

        //noinspection ConstantConditions
        if (!sharedViewModel.getIsConnected().getValue()) {

            sharedXRange.setMin(new Date(E4SessionData.getInstance().getInitialTime()));

            edaLineData.append(E4SessionData.getInstance().getGsrTimestamps(), E4SessionData.getInstance().getGsr());
            hrLineData.append(E4SessionData.getInstance().getHrTimestamps(), E4SessionData.getInstance().getHr());
            tempLineData.append(Utils.condenseSkip(E4SessionData.getInstance().getTempTimestamps(), 55), Utils.condenseAverage(E4SessionData.getInstance().getTemp(), 55));

            // averages already calculated in AsyncTask
            accLineData.append(E4SessionData.getInstance().getAccMagTimestamps(), E4SessionData.getInstance().getAccMag());

            cleanedEdaLineData.append(E4SessionData.getInstance().getGsrTimestamps(), Utils.averageFilter(Utils.medianFilter(E4SessionData.getInstance().getGsr(), 23), 55));
            final IRenderableSeries edaLineSeries = sciChartBuilder.newLineSeries()
                    .withDataSeries(cleanedEdaLineData)
                    .withStrokeStyle(ColorUtil.Yellow, 1f, false)
                    .build();
            edaChart.getRenderableSeries().add(edaLineSeries);

            averagedHrLineData.append(E4SessionData.getInstance().getHrTimestamps(), Utils.averageFilter(E4SessionData.getInstance().getHr(), 111));
            final IRenderableSeries hrLineSeries = sciChartBuilder.newLineSeries()
                    .withDataSeries(averagedHrLineData)
                    .withStrokeStyle(ColorUtil.Yellow, 1f, false)
                    .build();
            hrChart.getRenderableSeries().add(hrLineSeries);

            for (double tag : E4SessionData.getInstance().getTags()) {
                VerticalLineAnnotation verticalLine = sciChartBuilder.newVerticalLineAnnotation()
                        .withPosition(tag, 0.5d)
                        .withStroke(2, ColorUtil.Orange)
                        .withVerticalGravity(Gravity.FILL_VERTICAL)
                        .withIsEditable(false)
                        .build();

                Collections.addAll(edaChart.getAnnotations(), verticalLine);
            }

            Collections.addAll(edaChart.getAnnotations(),
                    sciChartBuilder.newTextAnnotation()
                            .withX1(0.005)
                            .withY1(0.24)
                            .withCoordinateMode(AnnotationCoordinateMode.Relative)
                            .withHorizontalAnchorPoint(HorizontalAnchorPoint.Left)
                            .withVerticalAnchorPoint(VerticalAnchorPoint.Bottom)
                            .withText(E4SessionData.getInstance().getDescription())
                            .withBackgroundDrawableId(R.drawable.annotation_bg_1)
                            .withFontStyle(10, ColorUtil.White)
                            .build());


            final List<Float> ibis = E4SessionData.getInstance().getIbi();
            if (ibis.size() > 0) {
                final int hrvSDRR = Math.round(Utils.calcHrvSDRR(ibis));
                final int hrvSDNN = Math.round(Utils.calcHrvSDNN(ibis));
                final int hrvRMSSD = Math.round(Utils.calcHrvRMSSD(ibis));
                final int hrvSDSD = Math.round(Utils.calcHrvSDSD(ibis));
                final int hrvNN50 = Utils.calcHrvNN50(ibis);
                final int hrvNN20 = Utils.calcHrvNN20(ibis);

                final int nnIntervals = ibis.size() == 0 ? 1 : ibis.size();
                final int hrvpNN50 = 100 * hrvNN50 / nnIntervals;
                final int hrvpNN20 = 100 * hrvNN20 / nnIntervals;

                final String hrvText = String.format(Locale.getDefault(),
                        "NN20: %d %d%%\nNN50: %d %d%%\nRMSSD: %d ms\nSDRR: %d ms\nSDNN: %d ms\nSDSD: %d ms",
                        hrvNN20, hrvpNN20, hrvNN50, hrvpNN50, hrvRMSSD, hrvSDRR, hrvSDNN, hrvSDSD);

                Collections.addAll(hrChart.getAnnotations(),
                        sciChartBuilder.newTextAnnotation()
                                .withX1(0.005)
                                .withY1(0.47)
                                .withCoordinateMode(AnnotationCoordinateMode.Relative)
                                .withHorizontalAnchorPoint(HorizontalAnchorPoint.Left)
                                .withVerticalAnchorPoint(VerticalAnchorPoint.Bottom)
                                .withText(hrvText)
                                .withFontStyle(10, ColorUtil.White)
                                .withBackgroundDrawableId(R.drawable.annotation_bg_1)
                                .build());
            }

            edaChart.animateZoomExtents(500);

        } else { // display live sensor data
            edaLineData.setFifoCapacity(256);
            tempLineData.setFifoCapacity(256);
            hrLineData.setFifoCapacity(1024);
            accLineData.setFifoCapacity(1024);

            edaChart.getXAxes().getDefault().setAutoRange(AutoRange.Always);
            edaChart.getXAxes().getDefault().setVisibility(View.GONE);

            // we are showing blood volume pulse while streaming instead of heart rate
            hrChart.getYAxes().getDefault().setAxisTitle("BVP");

            // Create a watermark using a TextAnnotation
            final TextAnnotation annotation = sciChartBuilder.newTextAnnotation()
                    .withX1(0.5)
                    .withY1(0.5)
                    .withFontStyle(Typeface.DEFAULT_BOLD, 42, 0x22FFFFFF)
                    .withCoordinateMode(AnnotationCoordinateMode.Relative)
                    .withHorizontalAnchorPoint(HorizontalAnchorPoint.Center)
                    .withVerticalAnchorPoint(VerticalAnchorPoint.Center)
                    .withText("streaming")
                    .withTextGravity(Gravity.CENTER)
                    .build();
            // axis markers showing current values for heart rate and temperature
            Collections.addAll(edaChart.getAnnotations(), annotation);

            Collections.addAll(tempChart.getAnnotations(), tempAxisMarker);


            sharedViewModel.getTag().observe(owner, new Observer<Double>() {
                @Override
                public void onChanged(Double tag) {
                    Collections.addAll(edaChart.getAnnotations(), sciChartBuilder.newVerticalLineAnnotation()
                            .withPosition(tag, 0.5d)
                            .withStroke(2, ColorUtil.Orange)
                            .withVerticalGravity(Gravity.FILL_VERTICAL)
                            .withIsEditable(false)
                            .build());
                }
            });

            sharedViewModel.getCurrentBvp().observe(owner, new Observer<Float>() {
                int count = 0;

                @Override
                public void onChanged(Float lastBvp) {
                    if (count++ % 2 == 0)
                        hrLineData.append((double) Utils.getCurrentTimestamp(), lastBvp);
                }
            });

            sharedViewModel.getCurrentGsr().observe(owner, new Observer<Float>() {
                @Override
                public void onChanged(Float lastGsr) {
                    edaLineData.append((double) Utils.getCurrentTimestamp(), lastGsr);
                }
            });

            sharedViewModel.getCurrentTemp().observe(owner, new Observer<Float>() {
                @Override
                public void onChanged(Float temp) {
                    tempLineData.append((double) Utils.getCurrentTimestamp(), temp);
                    tempAxisMarker.setY1(temp);
                }
            });

            sharedViewModel.getCurrentAccMag().observe(owner, new Observer<Float>() {
                @Override
                public void onChanged(Float mag) {
                    accLineData.append((double) Utils.getCurrentTimestamp(), mag);
                }
            });

            edaChart.animateZoomExtents(500);

        }
    }

    public static class DateLabelProviderEx extends DateLabelProvider {
        @Override
        public String formatLabel(Comparable dataValue) {
            // return a formatting string for tick labels
            return Utils.getDurationAsString(Math.round((Double) dataValue));
        }
    }

}