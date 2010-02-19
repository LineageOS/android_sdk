/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.eclipse.org/org/documents/epl-v10.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.adt.gscripts;

/**
 * An {@link IViewRule} for android.widget.LinearLayout and all its derived classes.
 */
public class AndroidWidgetLinearLayoutRule extends BaseLayout {

    // ==== Drag'n'drop support ====

    DropFeedback onDropEnter(INode targetNode) {

        def bn = targetNode.getBounds();
        if (!bn.isValid()) {
            return;
        }

        boolean isVertical = targetNode.getStringAttr("orientation") == "vertical";

        // Prepare a list of insertion points: X coords for horizontal, Y for vertical.
        // Each list is a tuple: 0=pixel coordinate, 1=index of children or -1 for "at end".
        def indexes = [ ] ;

        int last = 0;
        int pos = 0;
        targetNode.getChildren().each {
            def bc = it.getBounds();
            if (bc.isValid()) {
                int v = isVertical ? bc.y : bc.x;
                v = (last + v) / 2;
                indexes.add( [v, pos++] );

                last = isVertical ? (bc.y + bc.h) : (bc.x + bc.w);
            }
        }

        int v = isVertical ? bn.h : bn.w;
        v = (last + v) / 2;
        indexes.add( [v, -1] );

        return new DropFeedback(
          [ "p": null,
            "isVertical": isVertical,
            "indexes": indexes,
            "curr_x": null,
            "curr_y": null,
            "insert_pos": -1
          ],
          {
            gc, node, feedback ->
            // Paint closure for the LinearLayout.

            Rect b = node.getBounds();
            if (!b.isValid()) {
                return;
            }

            gc.setForeground(gc.registerColor(0x00FFFF00));

            gc.setLineStyle(IGraphics.LineStyle.LINE_SOLID);
            gc.setLineWidth(2);
            gc.drawRect(b);

            gc.setLineStyle(IGraphics.LineStyle.LINE_DOT);
            gc.setLineWidth(1);

            indexes.each {
                int i = it[0];
                if (isVertical) {
                    // draw horizontal lines
                    gc.drawLine(b.x, i, b.x + b.w, i);
                } else {
                    // draw vertical lines
                    gc.drawLine(i, b.y, i, b.y + b.h);
                }
            }

            def curr_x = feedback.userData.curr_x;
            def curr_y = feedback.userData.curr_y;

            if (curr_x != null && curr_y != null) {
                gc.setLineStyle(IGraphics.LineStyle.LINE_SOLID);
                gc.setLineWidth(2);

                int x = curr_x;
                int y = curr_y;
                gc.drawLine(x - 10, y - 10, x + 10, y + 10);
                gc.drawLine(x + 10, y - 10, x - 10, y + 10);
                gc.drawRect(x - 10, y - 10, x + 10, y + 10);
            }
        })
    }

    DropFeedback onDropMove(INode targetNode, DropFeedback feedback, Point p) {
        def data = feedback.userData;
        data.p = p;

        Rect b = targetNode.getBounds();
        if (!b.isValid()) {
            return feedback;
        }

        boolean isVertical = data.isVertical;

        int bestDist = Integer.MAX_VALUE;
        int bestIndex = Integer.MIN_VALUE;
        int bestPos = null;

        data.indexes.each {
            int i = it[0];
            int pos = it[1];
            if (bestDist > 0) {
                int dist = (isVertical ? p.y : p.x) - i;
                if (dist < 0) dist = - dist;
                if (dist < bestDist) {
                    bestDist = dist;
                    bestIndex = i;
                    bestPos = pos;
                }
            }
        }

        if (bestIndex != Integer.MIN_VALUE) {
            def old_x = data.curr_x;
            def old_y = data.curr_y;

            if (isVertical) {
                data.curr_x = b.x + b.w / 2;
                data.curr_y = bestIndex;
            } else {
                data.curr_x = bestIndex;
                data.curr_y = b.y + b.h / 2;
            }

            data.insert_pos = bestPos;

            feedback.requestPaint = (old_x != data.curr_x) || (old_y != data.curr_y);
        }

        return feedback;
    }

    void onDropped(String fqcn, INode targetNode, DropFeedback feedback, Point p) {

        Rect b = targetNode.getBounds();
        if (!b.isValid()) {
            return;
        }

        int x = p.x - b.x;
        int y = p.y - b.y;

        int insert_pos = feedback.userData.insert_pos;

        targetNode.debugPrintf("Linear.drop: add ${fqcn} at position ${insert_pos}");

        targetNode.editXml("Add child to LinearLayout") {
            INode e = targetNode.insertChildAt(fqcn, insert_pos);
            // TODO adjust attributes?
        }
    }

    void onDropLeave(INode targetNode, DropFeedback feedback) {
        // ignore
    }
}
