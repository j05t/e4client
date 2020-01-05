package com.jstappdev.e4client.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.jstappdev.e4client.MainActivity;
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
import com.scichart.charting.visuals.axes.IAxis;
import com.scichart.charting.visuals.renderableSeries.IRenderableSeries;
import com.scichart.charting.visuals.synchronization.SciChartVerticalGroup;
import com.scichart.data.model.DateRange;
import com.scichart.data.model.DoubleRange;
import com.scichart.drawing.utility.ColorUtil;
import com.scichart.extensions.builders.SciChartBuilder;

import java.util.Collections;
import java.util.Date;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ChartsFragment extends Fragment {

    private static final String hrAxisTitle = "HR";
    private static final String edaAxisTitle = "EDA";
    private static final String tempAxisTitle = "Temp";

    private static final int AXIS_MARKER_COLOR = 0xFFFFA500;
    private static final int HRV_MARKER_COLOR = 0x00FFA500;
    private static float averageHr = -1.0f;
    private final SciChartVerticalGroup verticalGroup = new SciChartVerticalGroup();
    private final DateRange sharedXRange = new DateRange();

    // @BindView(R.id.bvpChart)
    //SciChartSurface bvpChart;
    @BindView(R.id.edaChart)
    SciChartSurface edaChart;
    @BindView(R.id.hrChart)
    SciChartSurface hrChart;
    @BindView(R.id.tempChart)
    SciChartSurface tempChart;

    private SharedViewModel sharedViewModel;
    private SciChartBuilder sciChartBuilder;

    private XyDataSeries<Double, Float> edaLineData;
    private XyDataSeries<Double, Float> hrLineData;
    private XyDataSeries<Double, Float> cleanedGsrLineData;
    private XyDataSeries<Double, Float> tempLineData;

    private AxisMarkerAnnotation hrAxisMarker;
    private AxisMarkerAnnotation hrvAxisMarker;
    private AxisMarkerAnnotation tempAxisMarker;

    private boolean isVertical;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

        Display display = ((WindowManager) MainActivity.context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int rotation = display.getRotation();
        isVertical = rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180;

        Log.d(MainActivity.TAG, "orientation: " + requireActivity().getRequestedOrientation());

        sharedViewModel = ViewModelProviders.of(Objects.requireNonNull(requireActivity())).get(SharedViewModel.class);

        final View root = inflater.inflate(R.layout.fragment_charts, container, false);

        ButterKnife.bind(this, root);

        SciChartBuilder.init(requireActivity());
        sciChartBuilder = SciChartBuilder.instance();

        edaLineData = sciChartBuilder.newXyDataSeries(Double.class, Float.class).build();
        hrLineData = sciChartBuilder.newXyDataSeries(Double.class, Float.class).build();
        cleanedGsrLineData = sciChartBuilder.newXyDataSeries(Double.class, Float.class).build();
        tempLineData = sciChartBuilder.newXyDataSeries(Double.class, Float.class).build();

        hrAxisMarker = sciChartBuilder.newAxisMarkerAnnotation()
                .withY1(0d).withBackgroundColor(AXIS_MARKER_COLOR).build();
        hrvAxisMarker = sciChartBuilder.newAxisMarkerAnnotation()
                .withY1(0d).withBackgroundColor(HRV_MARKER_COLOR).build();
        tempAxisMarker = sciChartBuilder.newAxisMarkerAnnotation()
                .withY1(0d).withBackgroundColor(AXIS_MARKER_COLOR).build();

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

    private void setupChart(SciChartSurface chartSurface, final String yAxisTitle, XyDataSeries<Double, Float> lineData, boolean isFirstPane) {

        final IAxis xAxis = sciChartBuilder.newDateAxis()
                .withVisibleRange(sharedXRange)
                .withDrawMinorGridLines(false)
                .withGrowBy(0, 0.1)
                .withVisibility(isFirstPane ? View.VISIBLE : View.GONE)
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
    }

    @SuppressLint("DefaultLocale")
    private void setupCharts() {
        final LifecycleOwner owner = getViewLifecycleOwner();

        setupChart(edaChart, edaAxisTitle, edaLineData, true);
        setupChart(hrChart, hrAxisTitle, hrLineData, false);
        setupChart(tempChart, tempAxisTitle, tempLineData, false);

        verticalGroup.addSurfaceToGroup(edaChart);
        verticalGroup.addSurfaceToGroup(hrChart);
        verticalGroup.addSurfaceToGroup(tempChart);

        //noinspection ConstantConditions
        if (!sharedViewModel.getIsConnected().getValue()) {

            sharedXRange.setMin(new Date(E4SessionData.getInstance().getInitialTime()));

            Collections.addAll(edaChart.getChartModifiers(), sciChartBuilder.newModifierGroup()
                    .withXAxisDragModifier().build()
                    .withZoomPanModifier().withReceiveHandledEvents(true).withXyDirection(Direction2D.XDirection).build()
                    .withZoomExtentsModifier().build()
                    .build());

            edaLineData.append(E4SessionData.getInstance().getGsrTimestamps(), E4SessionData.getInstance().getGsr());
            hrLineData.append(E4SessionData.getInstance().getHrTimestamps(), E4SessionData.getInstance().getHr());
            tempLineData.append(E4SessionData.getInstance().getTempTimestamps(), E4SessionData.getInstance().getTemp());

            cleanedGsrLineData.append(E4SessionData.getInstance().getGsrTimestamps(), Utils.removeAnomalies(E4SessionData.getInstance().getGsr()));
            final IRenderableSeries lineSeries = sciChartBuilder.newLineSeries()
                    .withDataSeries(cleanedGsrLineData)
                    .withStrokeStyle(ColorUtil.Green, 1f, false)
                    .build();
            edaChart.getRenderableSeries().add(lineSeries);

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
                            .withY1(isVertical ? 0.13 : 0.24)
                            .withCoordinateMode(AnnotationCoordinateMode.Relative)
                            .withHorizontalAnchorPoint(HorizontalAnchorPoint.Left)
                            .withVerticalAnchorPoint(VerticalAnchorPoint.Bottom)
                            .withText(E4SessionData.getInstance().getDescription())
                            .withBackgroundDrawableId(R.drawable.annotation_bg_1)
                            .withFontStyle(10, ColorUtil.White)
                            .build());


            final float hrvSDRR = Utils.calcHrvSDRR(E4SessionData.getInstance().getIbi());
            final float hrvSDNN = Utils.calcHrvSDNN(E4SessionData.getInstance().getIbi());
            final float hrvRMSSD = Utils.calcHrvRMSSD(E4SessionData.getInstance().getIbi());
            final float hrvSDSD = Utils.calcHrvSDSD(E4SessionData.getInstance().getIbi());
            final int hrvNN50 = Utils.calcHrvNN50(E4SessionData.getInstance().getIbi());
            final int hrvNN20 = Utils.calcHrvNN20(E4SessionData.getInstance().getIbi());

            int nnIntervals = E4SessionData.getInstance().getIbi().size();
            if (nnIntervals == 0) nnIntervals = 1;

            final int hrvpNN50 = 100 * hrvNN50 / nnIntervals;
            final int hrvpNN20 = 100 * hrvNN20 / nnIntervals;

            final String hrvText = String.format(
                    "NN20: %d %d%%\nNN50: %d %d%%\nRMSSD: %.0f ms\nSDRR: %.0f ms\nSDNN: %.0f ms\nSDSD: %.0f ms",
                    hrvNN20, hrvpNN20, hrvNN50, hrvpNN50, hrvRMSSD, hrvSDRR, hrvSDNN, hrvSDSD);

            Collections.addAll(hrChart.getAnnotations(),
                    sciChartBuilder.newTextAnnotation()
                            .withX1(0.005)
                            .withY1(isVertical ? 0.48 : 0.8)
                            .withCoordinateMode(AnnotationCoordinateMode.Relative)
                            .withHorizontalAnchorPoint(HorizontalAnchorPoint.Left)
                            .withVerticalAnchorPoint(VerticalAnchorPoint.Bottom)
                            .withText(hrvText)
                            .withFontStyle(10, ColorUtil.White)
                            .withBackgroundDrawableId(R.drawable.annotation_bg_1)
                            .build());

            edaChart.animateZoomExtents(500);

        } else { // display live sensor data

            // Create a watermark using a TextAnnotation
            TextAnnotation textAnnotation = sciChartBuilder.newTextAnnotation()
                    .withX1(0.5)
                    .withY1(0.5)
                    .withFontStyle(Typeface.DEFAULT_BOLD, 42, 0x22FFFFFF)
                    .withCoordinateMode(AnnotationCoordinateMode.Relative)
                    .withHorizontalAnchorPoint(HorizontalAnchorPoint.Center)
                    .withVerticalAnchorPoint(VerticalAnchorPoint.Center)
                    .withText("Streaming")
                    .withTextGravity(Gravity.CENTER)
                    .build();
            // Add the annotation to the AnnotationsCollection of a surface
            Collections.addAll(edaChart.getAnnotations(), textAnnotation);


            // axis markers showing current values for heart rate and temperature
            Collections.addAll(hrChart.getAnnotations(), hrAxisMarker, hrvAxisMarker);
            Collections.addAll(tempChart.getAnnotations(), tempAxisMarker);


            sharedViewModel.getTag().observe(owner, new Observer<Double>() {
                @Override
                public void onChanged(Double tag) {
                    VerticalLineAnnotation verticalLine = sciChartBuilder.newVerticalLineAnnotation()
                            .withPosition(tag, 0.5d)
                            .withStroke(2, ColorUtil.Orange)
                            .withVerticalGravity(Gravity.FILL_VERTICAL)
                            .withIsEditable(false)
                            .build();

                    Collections.addAll(edaChart.getAnnotations(), verticalLine);
                }
            });

            sharedViewModel.getLastIbi().observe(owner, new Observer<Integer>() {
                @Override
                public void onChanged(Integer lastIbi) {
                    try {
                        final float currentHr = 60.0f / E4SessionData.getInstance().getIbi().get(lastIbi);

                        // heart rate may theoretically reach 600, but we assume 300 max
                        // https://www.ncbi.nlm.nih.gov/pmc/articles/PMC3273956/
                        if (averageHr != 0.0f && currentHr < 300f) {
                            averageHr = 0.8f * averageHr + 0.2f * currentHr;
                        }

                        hrLineData.append(E4SessionData.getInstance().getHrTimestamps().get(lastIbi), currentHr);
                        hrAxisMarker.setY1(averageHr);
                    } catch (Exception e) {
                        Log.e(MainActivity.TAG, "updateCharts() " + e.getMessage());
                    }
                }
            });

            sharedViewModel.getLastGsr().observe(owner, new Observer<Integer>() {
                @Override
                public void onChanged(Integer lastGsr) {
                    try {
                        edaLineData.append(E4SessionData.getInstance().getGsrTimestamps().get(lastGsr), E4SessionData.getInstance().getGsr().get(lastGsr));
                    } catch (Exception e) {
                        Log.e(MainActivity.TAG, "updateCharts() " + e.getMessage());
                    }
                }
            });

            sharedViewModel.getLastTemp().observe(owner, new Observer<Integer>() {
                @Override
                public void onChanged(Integer lastTemp) {
                    try {
                        tempLineData.append(E4SessionData.getInstance().getTempTimestamps().get(lastTemp), E4SessionData.getInstance().getTemp().get(lastTemp));
                        tempAxisMarker.setY1(E4SessionData.getInstance().getTemp().get(lastTemp));
                    } catch (Exception e) {
                        Log.e(MainActivity.TAG, "updateCharts() " + e.getMessage());
                    }
                }
            });

        }
    }


    public static class DateLabelProviderEx extends DateLabelProvider {
        @Override
        public String formatLabel(Comparable dataValue) {
            // return a formatting string for tick labels
            return Utils.getDuration(Math.round((Double) dataValue));
        }
    }

}