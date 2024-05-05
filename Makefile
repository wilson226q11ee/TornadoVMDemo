all: build

# Variable passed for the build process. List of backend/s to use { opencl, ptx, spirv }. The default one is `opencl`.
# make BACKEND=<comma_separated_backend_list>
BACKEND ?= opencl


.PHONY: docs
