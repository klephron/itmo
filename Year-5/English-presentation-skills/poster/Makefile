MKDIR := mkdir -p
BUILD_DIR := build
LATEX := pdflatex
LATEX_OPTIONS := -verbose -file-line-error -synctex=1 -interaction=nonstopmode


.DEFAULT_GOAL := all
.PHONY := all setup clean

FORCE:

DIRS := $(sort $(BUILD_DIR))
$(sort $(DIRS)):
	$(MKDIR) $(@:dir/%=%)

$(BUILD_DIR)/%.pdf: %.tex FORCE | $(DIRS)
	$(LATEX) $(LATEX_OPTIONS) -output-directory=$(BUILD_DIR) $<


all:
	@$(MAKE) setup
	# need to be run twice to resolve symbols and display data correctly
	@$(MAKE) $(BUILD_DIR)/poster.pdf
	@$(MAKE) $(BUILD_DIR)/poster.pdf

# NOTE: it is a workaround to compile with -outdir options set to not default directory
# otherwise there is some issues with finding log and fmt files
setup:
	# fmtutil-user --all
	fmtutil-user --byfmt xelatex

clean:
	rm -rf $(BUILD_DIR) || true
